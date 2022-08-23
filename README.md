<img align="right" src="assets/logo.webp" width="20%">

## Rescrape
![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/bluelhf/Rescrape/Java%20CI%20with%20Maven/main?color=FFFFFF)
<br align="clear"/>

```java
RescrapeAPI.withUserAgent("my unique user agent")
    .scrape(search(r("aww")).keywords("dog").limit(100))
    .thenAccept(urls -> urls.forEach(err::println)).join();
```
Rescrape is a Reddit media scraping API for Java.
It has support for full advanced search, but also strips
away redundant features from Reddit's search API, instead
opting for simplicity and ease-of-use.

The Rescrape API matches Reddit's API endpoints almost exactly,
except in a few ways, listed here:

- NSFW content is allowed by default
    - This is done to avoid the mess caused by Reddit's `nsfw:yes` query option and
      `include_over_18` API option. NSFW content can be filtered out by adding `.nsfw(false)` to the search query.

- Some elements have friendlier names:
    - The time bound, Reddit's `t` option, is called `Time` instead.
    - The type values `sr` and `link` are called `SUBREDDITS` and `POSTS`, respectively.


## Usage

### Obtaining an API instance
To use Rescrape, a `RescrapeAPI` instance is needed. `RescrapeAPI` objects
hold crucial configuration options, like what to do with the base Reddit API
connection, and which executor to use to perform media extraction on each result.

`RescrapeAPI` follows a builder pattern, and its values can be configured even
after it has been created.

```java
final RescrapeAPI api = RescrapeAPI.withUserAgent("my user agent")
        .executes(Executors.newWorkStealingPool(16))
```

### Forming searches
All searches consist of some parameters, called the search options,
along with a search query.

#### Search objects and their parameters

Rescrape represents search URLs with corresponding `Search` objects.
These objects can be modified freely using their builder methods,
but their values cannot be read. 

> **Note**  
> A search object constructed using `Search.search(Query)` returns
> the top 25 most relevant posts of all time by  default.

Aside from their usual parameters like sort and result limit, searches also
hold a powerful, customizable **search query**.

#### Search queries
Reddit search queries consist of options, which are known key—value pairs
in the format `key:value`, logical operators, such as AND, NOT, and OR,
keywords, such as "cat" or "boobs", and parentheses.

Rescrape represents these queries `Part` objects,
organised into `Query` objects. Queries represent any collection
of the aforementioned query elements in parentheses. For example,
`(subreddit:foo AND NOT bar)` is a valid Query, but `subreddit:foo
AND NOT bar` is not — instead it is a collection of an option part,
two logical operator parts, and a keyword part.


The `Query` class holds a myriad of static methods for easily
generating valid queries for use with Reddit, namely methods for
option parts and keywords — logical operators are handled by `Part`
instances themselves.
```java
import static blue.lhf.rescrape.api.query.Query.*;

// A query for non-cat GIFs in r/aww
query(
    subreddit("aww"),
    not(keyword("cat")),
    url(".gif")
);
```




### Using Searches

The `RescrapeAPI` class holds several methods for scraping data, generally called
**scraper methods**.

All scraper methods generally return a `CompletableFuture<Collection<URL>>`, which
is completed with all scraped URLs after they have all been visited. Additionally,
some scraper methods support handling URLs as they are extracted via a `Consumer<URL>` parameter,
and further some of those also support handling exceptions as they appear via a `Consumer<Exception>` parameter.
The default operation for both of these parameters is to do nothing.

```java
// All possible parameters of RescrapeAPI#scrape are
// used here: URLs and exceptions are printed to STDERR
// as they happen, and the message "All URLs scraped." is
// sent when the scraper is finished.
api.scrape(search,
    System.err:println,
    Throwable::printStackTrace).thenRun(() -> {
        System.err.println("All URLs scraped.");
    }).join();
```
