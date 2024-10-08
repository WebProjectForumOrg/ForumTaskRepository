package com.example.forumproject.services;

import com.example.forumproject.exceptions.AuthorizationException;
import com.example.forumproject.exceptions.EntityNotFoundException;
import com.example.forumproject.helpers.AuthenticationHelper;
import com.example.forumproject.helpers.mapper.PostMapper;
import com.example.forumproject.helpers.specifications.PostMvcSpecification;
import com.example.forumproject.helpers.specifications.PostSpecification;
import com.example.forumproject.models.Post;
import com.example.forumproject.models.Tag;
import com.example.forumproject.models.User;
import com.example.forumproject.models.options.FilterOptions;
import com.example.forumproject.repositories.contracts.PostRepository;
import com.example.forumproject.repositories.contracts.UserRepository;
import com.example.forumproject.services.contracts.PostService;
import com.example.forumproject.services.contracts.UserService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {
    private static final String POST_ERROR_MESSAGE = "Only post creator can modify a post.";
    private static final String DELETE_POST_ERROR_MESSAGE = "Only post creator or admin or moderator can delete a post.";
    public static final String ADMIN_OR_LOGGER_ERROR = "Should be admin or logged in user to view other's posts likes";
    public static final int POST_LIST_SIZE = 10;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EntityManager entityManager;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, UserRepository userRepository, UserService userService, EntityManager entityManager) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Post> getPostById(int id) {
        return postRepository.findById(id);
    }

    @Override
    public Page<?> getAllPosts(User user, FilterOptions filterOptions, Pageable pageable) {
        Specification<Post> specification = PostSpecification.filterByOption(filterOptions);
        Page<Post> posts = postRepository.findAll(specification, pageable);
        if ("Admin".equals(user.getRole().getRoleName())) {
            return posts;
        } else {
            return posts.map(PostMapper::toUserDTO);
        }
    }

    @Override
    public Page<Post> getAllPosts(String title, String tag, int page, int size, String sortBy, String direction) {
        Specification<Post> spec = Specification.where(null);

        if (title != null && !title.isEmpty()) {
            spec = spec.and(PostMvcSpecification.hasTitle(title));
        }
        if (tag != null && !tag.isEmpty()) {
            spec = spec.and(PostMvcSpecification.hasTag(tag));
        }

        Sort sort = Sort.by(direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return postRepository.findAll(spec, pageable);
    }

    @Override
    public Post create(Post post, User user) {
        AuthenticationHelper.throwIfUserIsBlocked(user);
        return postRepository.save(post);
    }

    @Override
    public Post update(Post post, User user) {
        AuthenticationHelper.throwIfUserIsBlocked(user);
        checkModifyPermissions(post.getId(), user);
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public void delete(int id, User user) {
        AuthenticationHelper.throwIfUserIsBlocked(user);
        checkModifyPermissionsToDelete(id, user);
        Post postToDelete = postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Post", id));
        user.getPostsLiked().removeIf(post -> post.getId() == postToDelete.getId());
        postToDelete.getLikes().removeIf(likedUser -> likedUser.getId() == user.getId());
        userRepository.save(user);
        postRepository.save(postToDelete);
        postRepository.delete(postToDelete);
    }

    @Override
    @Transactional
    public void likePost(Post post, User user) {
        AuthenticationHelper.throwIfUserIsBlocked(user);
        Set<User> usersLikedPost = post.getLikes();
        Set<Post> postLikedByUser = user.getPostsLiked();
        if (usersLikedPost.contains(user)) {
            usersLikedPost.remove(user);
            postLikedByUser.remove(post);
            post.setLikesCount(post.getLikesCount() - 1);
        } else {
            usersLikedPost.add(user);
            postLikedByUser.add(post);
            post.setLikesCount(post.getLikesCount() + 1);
        }
        postRepository.save(post);
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public Set<Post> getUserPosts(int id) {
        User user = userService.getUserById(id).orElseThrow(() -> new EntityNotFoundException("User", id));
        return postRepository.findByCreatedBy(user);
    }

    @Override
    public Set<?> getUserLikedPosts(User loggedInUser, int id) {
        validateAccess(loggedInUser, id);
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User", id));
        Set<Post> likedPosts = user.getPostsLiked();
        if (loggedInUser.getRole().getRoleName().equals("Admin")) {
            return likedPosts;
        } else {
            return likedPosts.stream()
                    .map(PostMapper::toUserDTO).collect(Collectors.toSet());
        }
    }

    @Override
    public Post getMostLikedPost() {
        return postRepository.findAll().stream()
                .max(Comparator.comparingInt(post -> post.getLikes().size()))
                .orElse(null);
    }


    private void checkModifyPermissions(int postId, User user) {
        Post repositoryPost = postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("Post", postId));
        if (!repositoryPost.getCreatedBy().equals(user)) {
            throw new AuthorizationException(POST_ERROR_MESSAGE);
        }
    }

    private void checkModifyPermissionsToDelete(int postId, User user) {
        Post repositoryPost = postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("Post", postId));
        if (repositoryPost.getCreatedBy().equals(user)) {
            return;
        }
        String roleName = user.getRole().getRoleName();
        if (roleName.equals("Admin") || roleName.equals("Moderator")) {
            return;
        }
        throw new AuthorizationException(DELETE_POST_ERROR_MESSAGE);
    }

    private void validateAccess(User loggedUser, int id) {
        if (!loggedUser.getRole().getRoleName().equals("Admin") && loggedUser.getId() != id) {
            throw new AuthorizationException(ADMIN_OR_LOGGER_ERROR);
        }
    }

    @Override
    public int getPostCount() {
        return postRepository.findAll().size();
    }

    @Override
    public List<Post> getTenMostLikedPosts() {
        return postRepository.findAll().stream().sorted(Comparator.comparing(Post::getLikesCount).reversed())
                .limit(POST_LIST_SIZE)
                .collect(Collectors.toList());
    }

    @Override
    public List<Post> getTenMostCommentedPosts() {
        return postRepository.findAll().stream()
                .sorted(Comparator.comparing(Post::getCommentsCount).reversed())
                .limit(POST_LIST_SIZE)
                .collect(Collectors.toList());
    }

    @Override
    public List<Post> getTenMostRecentPosts() {
        return postRepository.findAll().stream()
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .limit(POST_LIST_SIZE)
                .collect(Collectors.toList());
    }

    @Override
    public List<Post> getPostsByTag(Tag tag) {
        return postRepository.findByTagsContains(tag);
    }
}
