package ru.ytkab0bp.sapil

import ru.ytkab0bp.sapil.APIRunner.RunnerConfig

object PlaceholderRepo {
    interface Api : APIRunner {
        @Method("posts")
        fun getPosts(callback: APICallback<List<Post>>)

        @Method("posts/{}")
        fun getPost(id: Int, callback: APICallback<Post>)

        @Method("comments")
        fun getComments(@Arg("postId") postId: Int, callback: APICallback<List<Comment>>)
    }

    private val api: Api = APILibrary.newRunner(
        Api::class.java,
        object : RunnerConfig {
            override fun getBaseURL(): String = "https://jsonplaceholder.typicode.com/"
        },
    )

    fun getPosts(callback: APICallback<List<Post>>) = api.getPosts(callback)

    fun getPost(id: Int, callback: APICallback<Post>) = api.getPost(id, callback)

    fun getComments(postId: Int, callback: APICallback<List<Comment>>) = api.getComments(postId, callback)

    class Post {
        @JvmField var userId: Int = 0
        @JvmField var id: Int = 0
        @JvmField var title: String? = null
        @JvmField var body: String? = null

        override fun toString(): String =
            "Post{userId=$userId, id=$id, title='$title', body='$body'}"
    }

    class Comment {
        @JvmField var postId: Int = 0
        @JvmField var id: Int = 0
        @JvmField var name: String? = null
        @JvmField var email: String? = null
        @JvmField var body: String? = null

        override fun toString(): String =
            "Comment{postId=$postId, id=$id, name='$name', email='$email', body='$body'}"
    }
}
