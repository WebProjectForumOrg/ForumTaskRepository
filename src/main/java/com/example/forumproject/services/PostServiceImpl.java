package com.example.forumproject.services;

import com.example.forumproject.exceptions.AuthorizationException;
import com.example.forumproject.exceptions.BlockedException;
import com.example.forumproject.exceptions.EntityNotFoundException;
import com.example.forumproject.exceptions.OperationAlreadyPerformedException;
import com.example.forumproject.helpers.AuthenticationHelper;
import com.example.forumproject.helpers.PostSpecification;
import com.example.forumproject.models.Post;
import com.example.forumproject.models.User;
import com.example.forumproject.models.options.FilterPostOptions;
import com.example.forumproject.repositories.contracts.PostRepository;
import com.example.forumproject.services.contracts.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PostServiceImpl implements PostService {
    private static final String POST_ERROR_MESSAGE = "Only post creator can modify a post.";
    private static final String DELETE_POST_ERROR_MESSAGE = "Only post creator or admin or moderator can delete a post.";
    private static final String MORE_THAN_ONCE_LIKED_ERROR = "The post should be liked only once";
    private static final String MORE_THAN_ONCE_REMOVE_LIKE_ERROR = "The like from post can be removed only once";
    private final PostRepository postRepository;
    @Autowired
    public PostServiceImpl(PostRepository postRepository){
        this.postRepository = postRepository;
    }

    @Override
    public Optional<Post> getPostById(int id) {
        return postRepository.findById(id);
    }

    @Override
    public List<Post> getAllPosts(FilterPostOptions filterPostOptions) {
        Specification<Post> specification = PostSpecification.filterByOption(filterPostOptions);
        return postRepository.findAll(specification);
    }

    @Override
    public Post create(Post post, User user) {
        AuthenticationHelper.checkUserBlockStatus(user);
        return postRepository.save(post);
    }

    @Override
    public Post update(Post post, User user) {

        AuthenticationHelper.checkUserBlockStatus(user);
        checkModifyPermissions(post.getId(), user);
        return postRepository.save(post);
    }

    @Override
    public void delete(int id, User user) {
        AuthenticationHelper.checkUserBlockStatus(user);
        checkModifyPermissionsToDelete(id, user);
        Post postToDelete = postRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Post", id));
        postRepository.delete(postToDelete);
    }

    //TODO put removeLikePost in if() instead of exception
    @Override
    public void likePost(Post post, User user) {
        AuthenticationHelper.checkUserBlockStatus(user);
        Set<User> usersLikedPost = post.getLikes();
        if (usersLikedPost.contains(user)){
            usersLikedPost.remove(user);
            post.setLikesCount(post.getLikesCount() - 1);
        } else {
            usersLikedPost.add(user);
            post.setLikesCount(post.getLikesCount() + 1);
        }
        postRepository.save(post);
    }

    //DONE //TODO method to check if the user is user or admin/moderator for user to edit which HE CREATED and post which HE CREATED  and for admin/moderator to delete posts ANY POST
    private void checkModifyPermissions(int postId, User user) {
        Post repositoryPost = postRepository.findById(postId).orElseThrow(()->new EntityNotFoundException("Post", postId));
        if (!repositoryPost.getCreatedBy().equals(user)) {
            throw new AuthorizationException(POST_ERROR_MESSAGE);
        }
    }
    private void checkModifyPermissionsToDelete(int postId, User user) {
        Post repositoryPost = postRepository.findById(postId).orElseThrow(()->new EntityNotFoundException("Post", postId));
        if (repositoryPost.getCreatedBy().equals(user)) {
            return;
        }
        String roleName = user.getRole().getRoleName();
        if (roleName.equals("Admin") || roleName.equals("Moderator")){
            return;
        }
        throw new AuthorizationException(DELETE_POST_ERROR_MESSAGE);
    }

    //should we leave it here or should we do it like we have by putting it in the authenticationhelper class
//    private void checkUserBlockStatus(User user){
//        if (user.getBlocked()){
//            throw new BlockedException("Username", user.getUsername());
//        }
//    }
}
