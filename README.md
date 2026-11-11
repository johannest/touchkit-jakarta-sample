# Vaadin 7.7 + TouchKit 4.1.0 on Jakarta EE 10 (Payara 6, JDK 17+)

A minimal sample showing that a Vaadin 7.7 / TouchKit 4.1.0 app runs on Payara 6
(Servlet 6.0). It has been tested on Payara Micro 6.2025.11, Corretto 21, compiled with `--release 17`.
It covers TouchKit navigation, form components, a server round trip and `@Push`.

## What it takes to get this working with Vaadin 7 and TK (3 things)

### 1. Use the Jakarta builds of Vaadin 7.7 (part of the Vaadin 7 extended maintenance)

| Replace (javax)   | With (jakarta)                         |
|-------------------|----------------------------------------|
| `vaadin-server`   | `vaadin-server-mpr-jakarta` (7.7.36+)  |
| `vaadin-push`     | `vaadin-push-jakarta` (7.7.47+)        |

`vaadin-themes` and `vaadin-shared` stay the same. `vaadin-client` and `vaadin-client-compiler`
are only needed for compiling the widgetset. Make them `provided`, and **exclude
`vaadin-server` from them**. Otherwise the javax `vaadin-server` gets onto the classpath,
or into the WAR, next to the Jakarta one. Run `mvn dependency:tree` and confirm that
`com.vaadin:vaadin-server` does not show up.

### 2. Replace `TouchKitServlet` with a Jakarta port

It is the **only** class in TouchKit that uses the Servlet API (checked with `jdeps`).
[`JakartaTouchKitServlet`](src/main/java/com/example/touchkit/JakartaTouchKitServlet.java)
is a 1:1 copy, with `javax.servlet` changed to `jakarta.servlet`. Use it wherever the
application extends or references `TouchKitServlet`, in `@WebServlet` or `web.xml`. The TouchKit
jar itself stays unchanged.

### 3. Two fixes that are needed regardless of Jakarta

You only see these once the servlet works. They would also break a javax deployment on
current Vaadin 7.7.x and current browsers:

* **jsoup incompatibility (server, HTTP 500 at bootstrap).** Recent 7.7.x versions ship jsoup 1.2x.
  TouchKit 4.1.0 calls two jsoup constructors that have since been removed, which throws
  `NoSuchMethodError: org.jsoup.nodes.DocumentType.<init>(String,String,String,String)`.
  The fix is patched copies of
  [`ViewPortSettings`](src/main/java/com/vaadin/addon/touchkit/settings/ViewPortSettings.java) and
  [`ApplicationCacheSettings`](src/main/java/com/vaadin/addon/touchkit/settings/ApplicationCacheSettings.java)
  under the same package. `WEB-INF/classes` loads before `WEB-INF/lib`, so these copies win.
  Each copy has a one-line change, marked `PATCH`.
* **HTML5 AppCache removed from browsers (client, blank page).** TouchKit's client calls
  `window.applicationCache`. Current Chrome / Android WebView, Firefox and Safari no longer
  have it, so the browser throws `Cannot read properties of undefined (reading 'addEventListener')`
  and the UI never renders. `JakartaTouchKitServlet` injects a small no-op stub into the
  bootstrap page, which reports status `UNCACHED`. A side effect is that TouchKit's offline mode / app cache
  can't work in modern browsers anymore, with or without this stub.

## Things to check in your real pom (real world project)

* **`vaadin-cdi` Use version 1.9.0 which is Vaadin 7 and Jakarta compatible.
* **Other add-ons** need checking one by one for `javax.*` server-side usage, for example:
  `jdeps -verbose:class some-addon.jar | grep 'javax\.'`
  Most UI add-ons use only Vaadin APIs and work as they are. Anything that touches
  `javax.servlet`, `javax.inject`, `javax.enterprise` or `javax.annotation` needs a Jakarta
  replacement or a port.
* `vaadin-server-mpr-jakarta` validates the **Vaadin commercial license** at runtime. The
  production server needs a license key (server/offline key). See the Vaadin license docs.

## Build & run

```bash
mvn package
java -jar payara-micro-6.2025.11.jar --deploy target/touchkit-jakarta-sample-1.0-SNAPSHOT.war --contextroot app
# open http://localhost:8080/app/
```
