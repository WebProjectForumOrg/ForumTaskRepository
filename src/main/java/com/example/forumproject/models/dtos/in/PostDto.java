package com.example.forumproject.models.dtos.in;
/*☻*/
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PostDto {
    @NotNull(message = "Title can't be empty")
    @Size(min = 16, max = 64, message = "Title should be between 16 and 64 symbols")
    private String title;
    @NotNull(message = "Title can't be empty")
    @Size(min = 32, max = 8192, message = "Title should be between 32 and 8192 symbols")
    private String content;

    @NotNull(message = "User ID can't be empty")
    private int userId;

    public PostDto() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
