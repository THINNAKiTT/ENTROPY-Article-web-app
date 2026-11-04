package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordsTest {

    @Test
    public void rightPasswordVerifies() {
        String stored = Passwords.hash("s3cret!");
        assertTrue(Passwords.verify("s3cret!", stored));
    }

    @Test
    public void wrongOrEmptyPasswordIsRejected() {
        String stored = Passwords.hash("s3cret!");
        assertFalse(Passwords.verify("s3cret", stored));
        assertFalse(Passwords.verify("", stored));
        assertFalse(Passwords.verify(null, stored));
    }

    @Test
    public void theStoredHashIsNotAPassword() {
        String stored = Passwords.hash("s3cret!");
        assertFalse("knowing the hash must not be enough to log in", Passwords.verify(stored, stored));
    }

    @Test
    public void samePasswordHashesDifferentlyEveryTime() {
        assertNotEquals(Passwords.hash("s3cret!"), Passwords.hash("s3cret!"));
    }

    @Test
    public void hashFitsTheDatabaseColumn() {
        assertTrue("password_hash is varchar(255)", Passwords.hash("s3cret!").length() < 255);
    }

    @Test
    public void garbageStoredHashIsRejected() {
        assertFalse(Passwords.verify("x", "not-a-hash"));
        assertFalse(Passwords.verify("x", null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPasswordCannotBeHashed() {
        Passwords.hash("   ");
    }

    @Test
    public void hashLooksLikeIterationsSaltHash() {
        assertEquals(3, Passwords.hash("s3cret!").split("\\$").length);
    }
}
