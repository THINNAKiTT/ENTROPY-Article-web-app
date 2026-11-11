# Entropy

A small article-sharing site: write articles, follow writers, like, save and comment.
Jakarta Faces + PrimeFaces (the look), Tailwind (layout), GlassFish 8, JDK 25, MariaDB/MySQL.

## Run it

```
mvn clean package # runs the unit tests and builds target/entropy.war
```

Deploy target/entropy.war to GlassFish; the app is then at http://localhost:8080/entropy/.
The build also runs Tailwind (npx @tailwindcss/cli, needs Node) to regenerate
src/main/webapp/resources/css/tailwind.css. No Node? Add -Dexec.skip=true.

The database is the local entropy database. To point the app somewhere else without editing code,
set ENTROPY_DB_URL, ENTROPY_DB_USER and ENTROPY_DB_PASSWORD (see util/Db.java).

## Tests

```
mvn test # unit tests: no database needed
mvn test -Dtest='*IT' # database tests: need the local database
```

The *IT tests create their data under a name prefix unique to the run and delete it afterwards
(support/TestData).

## How it works

```
browser -> PrettyUrlFilter -> .xhtml page -> bean (controller) -> Dao -> Db (JDBC) -> MariaDB
```

One database, entropy, with 10 tables: users, articles, comments, article_likes, bookmarks,
follows, tags, article_tags, article_images, login_tokens. The base schema is not in the repo. It must have ON DELETE CASCADE:
article -> its comments, likes, bookmarks, tag links and pictures; comment -> its replies; user -> everything of theirs.

Pictures live in the database as MEDIUMBLOBs: users.avatar (+ avatar_type) and the article_images table.
db/blob-storage.sql adds them and db/login-tokens.sql adds login_tokens; run them once on an existing database
(mysql entropy < db/blob-storage.sql).
The old users.avatar_url column is no longer used and can be dropped.

Every Dao call opens its own connection (no pool). Saving an article together with its tags is one transaction.

## Pages and addresses

| Address | Page file | What it shows |
|---|---|---|
| / | index.xhtml | landing page |
| /latest | latest.xhtml | latest articles |
| /avatar/5 | AvatarServlet | a user's profile picture (from the database) |
| /image/12 | ImageServlet | an article picture (from the database) |
| /popular | popular.xhtml | most liked and commented |
| /following | following.xhtml | articles from people you follow |
| /saved | saved.xhtml | articles you saved (needs an account) |
| /search/some%20words | search.xhtml | search results |
| /tag/some-topic | tag.xhtml | articles with a topic |
| /hello-world-jdl2i53q | article.xhtml | one article: its title plus 8 random characters |
| /@username (/followers, /following) | profile.xhtml | a profile |
| /write (/write?id=5 edits) | write.xhtml | the editor (needs an account) |

Log in and Sign up are popups in the layout, not pages. Edit profile is a popup on the profile page with two
tabs: Profile (name, bio, picture) and Account (email, Change password popup, Remove account with a Yes/No
confirmation). Links typed in the editor without http(s):// get https:// added (util/Html).

- web/Urls.java is the one place that knows the address scheme. Pages build links with #{nav...}
(web/NavBean). Paths are relative to the context path (/entropy).
- web/PrettyUrlFilter forwards a clean address to the real file inside the server (/@alice ->
profile.xhtml?username=alice). web/PrettyViewHandler makes forms post back to the clean address, except on
write.xhtml and profile.xhtml: they have file uploads, and GlassFish drops the multipart body of a request that
the filter forwarded, so those two pages post to their real .xhtml address.
- A plain page
 /xyz is served by xyz.xhtml, so those names (popular, saved, tag, error ...) can never
be an article address.
- Not found, someone else's draft, or not your article to edit: HTTP 404, shown with
 error.xhtml.

## Rules the code follows

- Who can do what. There are no roles in the database and no admin. A visitor can read. A logged-in
member can also like, save, follow, comment and write. The owner of an item can also edit or delete their
article, delete any comment under it, see their own drafts, and edit their own profile. Every check is made
in the beans, not only by hiding buttons.
- Login needed. /write and /saved use userSessionBean.requireLogin: a visitor goes to /latest with the
log in popup open. Like, save, follow and comment from a visitor open the popup in place.
- Popular ranks by likes + 2 x comments. Reading time is 200 words a minute.
- An article's address is its title + 8 random characters and never changes after an edit.
- Tags are typed comma separated: lower case,
 # removed, no duplicates, at most 8.
- Replies go one level deep. The comment's author and the article's author can delete a comment.
- Article text is HTML from the editor (p:textEditor). util/Html cleans it with the OWASP sanitizer when it is
saved and again when it is shown, so only simple formatting, links and pictures get through. Older plain-text
articles still display (util/Html turns them into paragraphs).
- Pictures: PNG, JPEG or GIF up to 5 MB, found out from the bytes, not the file name (util/Images). Profile
pictures are cropped to a 256 px square. Article pictures bigger than 1600 px are shrunk. An article keeps up to 30.
A picture is uploaded while writing; it is tied to the article when the article is saved, and unsaved ones are
deleted after a day. Article text stores the picture as /image/ID without the context path (util/Html
withContext / withoutContext).
- Limits: title 200, article 100,000 (HTML), comment 5,000 characters. Lists show up to 50 articles
(100 for saved and profile).
- Passwords: PBKDF2-SHA256, 65,536 rounds, random salt, stored as
 iterations$salt$hash. The session id is
replaced at log in and sign up. Sessions last 30 minutes and live in a cookie only.

## Cookies and local storage

- Cookie entropy_login (web/Cookies, UserSessionBean): a login that survives a server restart or the 30 minute
session timeout. Logging in (with "Keep me logged in" ticked, the default) or signing up stores a random
selector.validator pair in the cookie (30 days, HttpOnly) and, in the login_tokens table, the selector and the
SHA-256 of the validator. When a new session starts and the cookie matches an unexpired row, the user is logged in
again (UserSessionBean.init). Logging out, changing the password or removing the account deletes the rows.
The cookie never holds the password. The only other cookie is the session cookie.
- Local storage, key entropy.draft.new or entropy.draft.ID (write.xhtml script): a copy of the title, topics and text
of the article being written, saved every 3 seconds and when the page closes. Reopening the editor offers
Restore / Discard when the saved copy differs from what the page loaded. A successful Publish, Save draft or
Delete sets the flash value clearDraft and the layout removes that key.
- Local storage, key entropy.notice: set when "Got it" is pressed on the notice (comp:cookieNotice) that
explains the two points above.

## Where things are

Code is grouped by feature. A feature folder has model/ (the data), dao/ (the SQL) and controller/
(the beans the pages talk to), for whichever of those the feature has.

```
src/main/java
users/ User, UserDao, LoginTokenDao, UserSessionBean (log in/out), ProfileBean, AvatarServlet
articles/ Article, ArticleDao, ArticleListBean (base of every list page), LatestBean, PopularBean,
SearchBean, ArticleBean, EditorBean
comments/ Comment, CommentDao, CommentsBean
follows/ FollowDao, FollowingBean
likes/ LikeDao
bookmarks/ BookmarkDao, SavedBean
tags/ TagCount, TagDao, TagBean, TopicsBean
images/ ArticleImage, ImageDao, ImageServlet (article pictures)
landing/ LandingBean (the / page)
util/ Db, Passwords, Texts, Html, Images, Picture (no web code)
web/ Faces, Cookies, Urls, NavBean, PrettyUrlFilter, PrettyViewHandler (Faces/servlet plumbing)

db/ blob-storage.sql, login-tokens.sql (upgrades for pictures and persistent logins)

src/main/webapp
*.xhtml one file per page (see the table)
WEB-INF/templates/layout.xhtml header, popups, footer: every page uses it
resources/components/ reusable pieces, used as <comp:name>
resources/css/input.css Tailwind input (tailwind.css is generated from it)

src/test/java unit tests (*Test) and database tests (*IT)
```

A list page bean extends ArticleListBean and only overrides fetch().

## Writing pages

A page uses <ui:composition template="/WEB-INF/templates/layout.xhtml"> and fills title and content.
A page that needs view parameters or view actions puts its own <f:metadata> in the page file, outside the
ui:composition: GlassFish ignores it inside the shared template.

In the layout:

- Open the popups with
 PF('loginDialog').show() / PF('signupDialog').show().
- Show messages by adding
 :growl to update.
- Ask for confirmation by putting
 <p:confirm .../> inside a p:commandButton.
- The whole header is one form, because a command component outside a form makes Faces warn.

Components (<comp:name>):

| Component | Use | Attributes |
|---|---|---|
| articleGrid | cards with Like / Comments / Save | items, owner (the bean with toggleLike(id), toggleBookmark(id)), facet whenEmpty |
| emptyState | "nothing here" / "not found" card | icon, title, text, actionLabel, actionIcon, actionHrefor actionOnclick |
| feedLayout | list on the left, Topics panel on the right | showTopics (default true) |
| feedTabs | Latest / Popular tabs | active: latest or popular |
| topicsPanel | the most used tags | none |
| tagList | row of tag chips | tags, styleClass |
| cookieNotice | bottom notice about the cookie and local storage, already in the layout | none |
| userAvatar | round picture or initial | label, imageUrl, size: normal, large, xlarge |
| userCard | one person | user, showProfileButton |
| loginDialog, signupDialog | the two popups, already in the layout | none |

Popups open fast on purpose: no showEffect/hideEffect on the dialogs (PrimeFaces' effect takes 400 ms); input.css
fades them in over 0.12 s instead.

The editor on write.xhtml is p:textEditor with secure="false" (util/Html does the cleaning) and its own toolbar,
without the built-in image button, which would paste the picture into the text as base64. Pictures come from
the p:fileUpload (multiple, one at a time) and the Insert button puts them at the cursor.

Styling: PrimeFaces (theme saga-blue) styles the widgets; Tailwind only does page layout and spacing.
Tailwind's preflight is off so it doesn't fight PrimeFaces. The PrimeFaces theme overrides margins and classes
set on its own components, so put spacing on a plain wrapper (div, span, form). Use a plain div for an
anchor such as #comments: a JSF id inside a form gets the form's name in front of it.