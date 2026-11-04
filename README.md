# Entropy

A small article-sharing site: write articles, follow writers, like, save and comment.
Jakarta Faces + PrimeFaces (the look), Tailwind (layout), GlassFish 8, JDK 25, MariaDB/MySQL.

## Run it

```
mvn clean package        # runs the unit tests and builds target/entropy.war
```

Deploy `target/entropy.war` to GlassFish; the app is then at `http://localhost:8080/entropy/`.
The build also runs Tailwind (`npx @tailwindcss/cli`, needs Node) to regenerate
`src/main/webapp/resources/css/tailwind.css`. No Node? Add `-Dexec.skip=true`.

The database is the local `entropy` database. To point the app somewhere else without editing code,
set `ENTROPY_DB_URL`, `ENTROPY_DB_USER` and `ENTROPY_DB_PASSWORD` (see `util/Db.java`).

## Tests

```
mvn test                       # unit tests: no database needed
mvn test -Dtest='*IT'          # database tests: need the local database, clean up after themselves
```

## Pages and addresses

| Address | Page file | What it shows |
|---|---|---|
| `/` | `index.xhtml` | latest articles |
| `/popular` | `popular.xhtml` | most liked and commented |
| `/following` | `following.xhtml` | articles from people you follow |
| `/saved` | `saved.xhtml` | articles you saved (needs an account) |
| `/search/some%20words` | `search.xhtml` | search results |
| `/tag/some-topic` | `tag.xhtml` | articles with a topic |
| `/hello-world-jdl2i53q` | `article.xhtml` | one article: its title plus 8 random characters |
| `/@username` (`/followers`, `/following`) | `profile.xhtml` | a profile |
| `/write` (`/write?id=5` edits) | `write.xhtml` | the editor (needs an account) |

Log in and Sign up are popups in the layout, not pages. `web/Urls.java` is the one place that knows the
address scheme; `web/PrettyUrlFilter.java` makes the clean addresses work.

## Where things are

Code is grouped by feature. A feature folder has `model/` (the data), `dao/` (the SQL) and `controller/`
(the beans the pages talk to), for whichever of those the feature has.

```
src/main/java
  users/      User, UserDao, UserSessionBean (log in/out), ProfileBean
  articles/   Article, ArticleDao, ArticleListBean (base of every list page), HomeBean, PopularBean,
              SearchBean, ArticleBean, EditorBean
  comments/   Comment, CommentDao, CommentsBean
  follows/    FollowDao, FollowingBean
  likes/      LikeDao
  bookmarks/  BookmarkDao, SavedBean
  tags/       TagCount, TagDao, TagBean, TopicsBean
  util/       Db, Passwords, Texts          (no web code)
  web/        Faces, Urls, NavBean, PrettyUrlFilter, PrettyViewHandler   (Faces/servlet plumbing)

src/main/webapp
  *.xhtml                     one file per page (see the table)
  WEB-INF/templates/layout.xhtml   header, popups, footer: every page uses it
  resources/components/       reusable pieces, used as <comp:name>:
                              articleGrid, emptyState, feedLayout, feedTabs, topicsPanel, tagList,
                              userAvatar, userCard, loginDialog, signupDialog
  resources/css/input.css     Tailwind input (tailwind.css is generated from it)

src/test/java                 unit tests (*Test) and database tests (*IT)
```

A page that needs view parameters or view actions puts its own `<f:metadata>` in the page file, outside the
`ui:composition`: GlassFish ignores it inside the shared template.
