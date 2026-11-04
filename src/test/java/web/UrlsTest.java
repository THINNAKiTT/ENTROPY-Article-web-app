package web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UrlsTest {

    // ---- an address from the browser becomes the page that serves it

    @Test
    public void articleAddressGoesToTheArticlePage() {
        assertEquals("/article.xhtml?slug=hello-world-jdl2i53q", Urls.resolve("/hello-world-jdl2i53q"));
        assertEquals("/article.xhtml?slug=33-2", Urls.resolve("/33-2"));
        assertEquals("/article.xhtml?slug=my-article", Urls.resolve("/my-article/"));
    }

    @Test
    public void profileAddresses() {
        assertEquals("/profile.xhtml?username=alias", Urls.resolve("/@alias"));
        assertEquals("/profile.xhtml?username=a_b_1", Urls.resolve("/@a_b_1"));
        assertEquals("/profile.xhtml?username=alias&tab=followers", Urls.resolve("/@alias/followers"));
        assertEquals("/profile.xhtml?username=alias&tab=following", Urls.resolve("/@alias/following"));
    }

    @Test
    public void everyListPageHasItsOwnFile() {
        assertEquals("/popular.xhtml", Urls.resolve("/popular"));
        assertEquals("/following.xhtml", Urls.resolve("/following"));
        assertEquals("/saved.xhtml", Urls.resolve("/saved"));
        assertEquals("/search.xhtml", Urls.resolve("/search"));
        assertEquals("/write.xhtml", Urls.resolve("/write"));
    }

    @Test
    public void topicAddress() {
        assertEquals("/tag.xhtml?slug=big-data", Urls.resolve("/tag/big-data"));
    }

    @Test
    public void thingsThatAreNotOurs() {
        assertNull("home is served by the welcome file", Urls.resolve("/"));
        assertNull(Urls.resolve("/article.xhtml"));
        assertNull(Urls.resolve("/resources/css/tailwind.css"));
        assertNull(Urls.resolve("/jakarta.faces.resource/primefaces.js.xhtml"));
        assertNull(Urls.resolve("/favicon.ico"));
        assertNull(Urls.resolve("/a/b/c"));
        assertNull("upper case is not a slug", Urls.resolve("/MyArticle"));
        assertNull(Urls.resolve("/@bad name"));
        assertNull(Urls.resolve("/@alias/nonsense"));
        assertNull("settings is a popup now, not a tab", Urls.resolve("/@alias/settings"));
        assertNull(Urls.resolve("/@alias/followers/x"));
        assertNull(Urls.resolve("/../etc/passwd"));
        assertNull(Urls.resolve("/tag/Big-Data"));
        assertNull(Urls.resolve("/tag/"));
        assertNull(Urls.resolve("/tag/a/b"));
        assertNull("a page name is not an article", Urls.resolve("/tag"));
        assertNull(Urls.resolve("/error"));
        assertNull(Urls.resolve("/index"));
        assertNull("the removed pages are no longer routes to a page", Urls.resolve("/people.xhtml"));
    }

    // ---- building addresses

    @Test
    public void buildingAddresses() {
        assertEquals("/", Urls.home());
        assertEquals("/popular", Urls.popular());
        assertEquals("/following", Urls.following());
        assertEquals("/saved", Urls.saved());
        assertEquals("/write", Urls.write());
        assertEquals("/write?id=5", Urls.edit(5L));
        assertEquals("/my-article", Urls.article("my-article"));
        assertEquals("/@alias", Urls.profile("alias"));
        assertEquals("/@alias/followers", Urls.profile("alias", "followers"));
        assertEquals("/@alias", Urls.profile("alias", "articles"));
    }

    @Test
    public void topicAndSearchAddresses() {
        assertEquals("/tag/big-data", Urls.tag("Big Data"));
        assertEquals("/tag/big-data", Urls.tag("big-data"));
        assertEquals("/tag/c-tips", Urls.tag("c++ tips"));
        assertEquals("/search/hello%20world", Urls.search("  hello world "));
        assertEquals("/", Urls.search("   "));
        assertEquals("/", Urls.search(null));
    }

    // ---- search: /search/{words}

    @Test
    public void searchAddressesCarryTheWordsInThePath() {
        assertEquals("a space is %20, not +", "/search/c%2B%2B%20tips", Urls.search("c++ tips"));
        assertEquals("accents and symbols are encoded", "/search/m%C3%A1s%20%26%20menos", Urls.search("más & menos"));
        assertEquals("a slash would look like another path part, so it counts as a space", "/search/a%20b", Urls.search("a/b"));
        assertEquals("/", Urls.search("///"));
        assertEquals("very long searches are cut", "/search/" + "x".repeat(100), Urls.search("x".repeat(300)));
    }

    @Test
    public void searchPageReceivesTheDecodedWords() {
        // the filter decodes the path (%20 -> space) before calling resolve, so resolve sees real characters
        assertEquals("/search.xhtml?q=hello+world", Urls.resolve("/search/hello world"));
        assertEquals("/search.xhtml?q=c%2B%2B+tips", Urls.resolve("/search/c++ tips"));
        assertEquals("/search.xhtml?q=m%C3%A1s", Urls.resolve("/search/más"));
        assertEquals("no words: the search page itself", "/search.xhtml", Urls.resolve("/search"));
        assertEquals("/search.xhtml", Urls.resolve("/search/"));
        assertEquals("/search.xhtml", Urls.resolve("/search/   "));
    }

    @Test
    public void whatWeBuildForASearchComesBackAsTheSameWords() throws Exception {
        for (String typed : new String[] {"hello world", "c++ tips", "más & menos", "50% off", "100%", "what?", "#java"}) {
            String address = Urls.search(typed);
            // exactly what PrettyUrlFilter does to the address it receives
            String decoded = java.net.URLDecoder.decode(address.replace("+", "%2B"), java.nio.charset.StandardCharsets.UTF_8);
            String target = Urls.resolve(decoded);
            String q = java.net.URLDecoder.decode(target.substring(target.indexOf("?q=") + 3), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(typed, typed.trim(), q);
        }
    }

    @Test
    public void everyAddressWeBuildIsOneWeUnderstand() {
        for (String page : new String[] {Urls.popular(), Urls.following(), Urls.saved(), Urls.write()}) {
            assertNotNull(page, Urls.resolve(page));
        }
        assertEquals("/article.xhtml?slug=my-article", Urls.resolve(Urls.article("my-article")));
        assertEquals("/profile.xhtml?username=alias&tab=followers", Urls.resolve(Urls.profile("alias", "followers")));
        assertEquals("/tag.xhtml?slug=big-data", Urls.resolve(Urls.tag("Big Data")));
    }
}
