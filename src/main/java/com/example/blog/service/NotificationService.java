package com.example.blog.service;

import com.example.blog.model.Comment;
import com.example.blog.model.Notification;
import com.example.blog.model.NotificationType;
import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> findByRecipient(User recipient) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipient.getId());
    }

    public long countUnreadByRecipientId(Long recipientId) {
        if (recipientId == null) {
            return 0;
        }
        return notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Transactional
    public void notifyForComment(Comment comment) {
        if (comment == null || comment.getAuthor() == null || comment.getPost() == null || comment.getPost().getAuthor() == null) {
            return;
        }

        User actor = comment.getAuthor();
        Post post = comment.getPost();
        Map<Long, Notification> notifications = new LinkedHashMap<>();

        User postAuthor = post.getAuthor();
        if (!postAuthor.getId().equals(actor.getId())) {
            notifications.put(postAuthor.getId(),
                    new Notification(NotificationType.POST_COMMENT, postAuthor, actor, post, comment));
        }

        if (comment.getParentComment() != null && comment.getParentComment().getAuthor() != null) {
            User parentAuthor = comment.getParentComment().getAuthor();
            if (!parentAuthor.getId().equals(actor.getId())) {
                notifications.put(parentAuthor.getId(),
                        new Notification(NotificationType.COMMENT_REPLY, parentAuthor, actor, post, comment));
            }
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications.values());
        }
    }

    @Transactional
    public void markAllAsRead(User recipient) {
        List<Notification> notifications = findByRecipient(recipient);
        boolean changed = false;
        for (Notification notification : notifications) {
            if (notification.isUnread()) {
                notification.markAsRead();
                changed = true;
            }
        }
        if (changed) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Transactional
    public String openNotification(Long notificationId, User recipient) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, recipient.getId())
                .orElseThrow(() -> new IllegalArgumentException("消息不存在或无权访问"));
        notification.markAsRead();
        notificationRepository.save(notification);
        return notification.getTargetPath();
    }

    @Transactional
    public void deleteByPostId(Long postId) {
        notificationRepository.deleteByPostId(postId);
    }
}
