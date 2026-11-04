package tags.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** TagDao.parse turns what someone typed into the tags to store. It needs no database. */
public class TagParseTest {

    @Test
    public void lowerCasesTrimsAndDropsHashesAndDuplicates() {
        assertEquals(List.of("java", "jsf", "big data"), TagDao.parse("Java, #JSF ,, java,  Big  Data "));
    }

    @Test
    public void atMostEightTags() {
        assertEquals(8, TagDao.parse("a,b,c,d,e,f,g,h,i,j").size());
    }

    @Test
    public void nothingUsableGivesNoTags() {
        assertTrue(TagDao.parse(null).isEmpty());
        assertTrue(TagDao.parse("").isEmpty());
        assertTrue(TagDao.parse(" , ,#, !!! ").isEmpty());
    }

    @Test
    public void longNamesAreCutTo50Characters() {
        assertEquals(50, TagDao.parse("x".repeat(80)).get(0).length());
    }
}
