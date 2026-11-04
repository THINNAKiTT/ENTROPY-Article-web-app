package users.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import support.TestData;
import users.model.User;

/** Needs the database: run with  mvn test -Dtest='*IT'  */
public class UserDaoIT {
    private final TestData data = new TestData();
    private final UserDao dao = new UserDao();

    @After
    public void cleanup() throws Exception {
        data.cleanup();
    }

    @Test
    public void insertFindAndAuthenticate() throws Exception {
        User u = data.user("a");
        assertTrue(u.getId() > 0);

        assertEquals(u.getUsername(), dao.findById(u.getId()).getUsername());
        assertEquals(u.getId(), dao.findByUsername(u.getUsername()).getId());
        assertEquals(u.getId(), dao.findByEmail(u.getEmail()).getId());
        assertNull(dao.findByUsername(data.prefix + "_nobody"));

        assertNotNull("by username", dao.authenticate(u.getUsername(), "s3cret!"));
        assertNotNull("by email", dao.authenticate(u.getEmail(), "s3cret!"));
        assertNull("wrong password", dao.authenticate(u.getUsername(), "nope"));
        assertNull("blank password", dao.authenticate(u.getUsername(), ""));
        assertNull("the stored hash is not a password", dao.authenticate(u.getUsername(), dao.findById(u.getId()).getPasswordHash()));
    }

    @Test
    public void displayNameFallsBackToUsername() throws Exception {
        User u = data.user("b");
        assertEquals(u.getUsername(), dao.findById(u.getId()).getDisplayName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertWithoutPasswordIsRejected() throws Exception {
        User u = new User();
        u.setUsername(data.prefix + "_nopw");
        u.setEmail(data.prefix + "_nopw@example.test");
        dao.insert(u);
    }

    @Test
    public void updateChangesDetailsAndOnlyChangesThePasswordWhenAsked() throws Exception {
        User u = data.user("c");

        User edit = new User();
        edit.setId(u.getId());
        edit.setDisplayName("New Name");
        edit.setBio("about me");
        dao.update(edit);
        User after = dao.findById(u.getId());
        assertEquals("New Name", after.getDisplayName());
        assertEquals("about me", after.getBio());
        assertNotNull("old password still works", dao.authenticate(u.getUsername(), "s3cret!"));

        edit.setRawPassword("n3w-pass");
        dao.update(edit);
        assertNotNull(dao.authenticate(u.getUsername(), "n3w-pass"));
        assertNull(dao.authenticate(u.getUsername(), "s3cret!"));
    }
}
