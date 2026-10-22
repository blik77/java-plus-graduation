package evm.common.port;

public interface CommentCleanupPort {

    void deleteAllByAuthorId(Long authorId);

}
