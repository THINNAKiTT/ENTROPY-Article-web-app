package follows.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import support.TestData;
import users.model.User;

/** Needs the database: run with  mvn test -Dtest='*IT'  */
public class FollowDaoIT {
    private final TestData data = new TestData();
    private final FollowDao dao = new FollowDao();

    @After
    public void cleanup() throws Exception {
        data.cleanup();
    }

    @Test
    public void followAndUnfollow() throws Exception {
        User alice = data.user("a"), bob = data.user("b");
        assertFalse(dao.isFollowing(bob.getId(), alice.getId()));

        dao.follow(bob.getId(), alice.getId());
        dao.follow(bob.getId(), alice.getId()); // twice: nothing happens
        assertTrue(dao.isFollowing(bob.getId(), alice.getId()));
        assertFalse("following is one way", dao.isFollowing(alice.getId(), bob.getId()));

        dao.unfollow(bob.getId(), alice.getId());
        assertFalse(dao.isFollowing(bob.getId(), alice.getId()));
    }

    @Test
    public void countsAndLists() throws Exception {
        User alice = data.user("a"), bob = data.user("b"), carol = data.user("c");
        dao.follow(bob.getId(), alice.getId());
        dao.follow(carol.getId(), alice.getId());
        dao.follow(alice.getId(), bob.getId());

        assertEquals(2, dao.countFollowers(alice.getId()));
        assertEquals(1, dao.countFollowing(alice.getId()));
        assertEquals(2, dao.findFollowers(alice.getId()).size());
        assertEquals(bob.getId(), dao.findFollowing(alice.getId()).get(0).getId());
        assertNull("the lists never carry password hashes", dao.findFollowers(alice.getId()).get(0).getPasswordHash());
    }

    @Test
    public void youCannotFollowYourselfAndNothingIsStored() throws Exception {
        User alice = data.user("a");
        try {
            dao.follow(alice.getId(), alice.getId());
            throw new AssertionError("should have been refused");
        } catch (IllegalArgumentException expected) {
            // fine
        }
        assertFalse(dao.isFollowing(alice.getId(), alice.getId()));
    }
}
