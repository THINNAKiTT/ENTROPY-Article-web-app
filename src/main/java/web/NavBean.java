package web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import util.Html;

@Named("nav")
@ApplicationScoped
public class NavBean {

    private String full(String path) {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + path;
    }

    public String getHome()      { return full(Urls.home()); }
    public String getLatest()    { return full(Urls.latest()); }
    public String getPopular()   { return full(Urls.popular()); }
    public String getFollowing() { return full(Urls.following()); }
    public String getSaved()     { return full(Urls.saved()); }
    public String getWrite()     { return full(Urls.write()); }

    public String edit(Long articleId)                  { return full(Urls.edit(articleId)); }
    public String article(String slug)                  { return full(Urls.article(slug)); }
    public String profile(String username)              { return full(Urls.profile(username)); }
    public String profile(String username, String tab)  { return full(Urls.profile(username, tab)); }
    public String tag(String name)                      { return full(Urls.tag(name)); }
    public String path(String path)                     { return path == null ? "" : full(path); }
    public String image(Long id)                        { return full("/image/" + id); }

    public String body(String content) {
        return Html.withContext(Html.render(content), full(""));
    }
}
