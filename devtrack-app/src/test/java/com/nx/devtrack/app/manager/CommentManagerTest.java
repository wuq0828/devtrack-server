package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.ActivityLogDao;
import com.nx.devtrack.app.dao.CommentDao;
import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.dao.UserDao;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.app.model.User;
import com.nx.devtrack.app.security.PermissionManager;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentManagerTest {

    private final CommentDao commentDao = mock(CommentDao.class);
    private final DefectDao defectDao = mock(DefectDao.class);
    private final ActivityLogDao activityLogDao = mock(ActivityLogDao.class);
    private final UserDao userDao = mock(UserDao.class);
    private final PermissionManager permissionManager = mock(PermissionManager.class);
    private final UserNameResolver userNameResolver = mock(UserNameResolver.class);
    private final NotificationManager notificationManager = mock(NotificationManager.class);

    private final CommentManager manager = new CommentManager(
            commentDao, defectDao, activityLogDao, userDao, permissionManager, userNameResolver, notificationManager);

    private Defect defect() {
        Defect d = new Defect();
        d.setProjectId(1L);
        return d;
    }

    @Test
    void addComment_withMention_notifiesMentionedUser() {
        when(defectDao.findById(10L)).thenReturn(Optional.of(defect()));
        when(userNameResolver.name(anyLong())).thenReturn("Alice");
        User bob = new User();
        bob.setId(99L);
        bob.setUsername("bob");
        when(userDao.findByUsername("bob")).thenReturn(bob);

        manager.addComment(10L, "请 @bob 看下这个", 1L);

        verify(notificationManager).notify(eq(99L), eq("MENTION"), anyString(), eq("DEFECT"), eq(10L));
    }

    @Test
    void addComment_mentionUnknownUser_noNotify() {
        when(defectDao.findById(10L)).thenReturn(Optional.of(defect()));
        when(userNameResolver.name(anyLong())).thenReturn("Alice");
        when(userDao.findByUsername("ghost")).thenReturn(null);

        manager.addComment(10L, "@ghost 在吗", 1L);

        verify(notificationManager, never()).notify(anyLong(), anyString(), anyString(), anyString(), anyLong());
    }
}
