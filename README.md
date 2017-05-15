# Trafficante : Strongly Typed API Server
[![Trafficante](https://avivcarmis.github.io/trafficante/trafficante-logo.jpg "Trafficante")](https://github.com/avivcarmis/trafficante "Trafficante")

------------

**Trafficante** library introduces a very simple and intuitive way to construct a strongly typed server with spring boot and swagger within seconds.
Trafficante requires JDK 1.8 or higher.

### Latest Release
------------
The most recent release is Trafficante 1.0.0, released May 15, 2017.

To add a dependency on Trafficante Library using Maven, use the following:
```
<dependency>
    <groupId>io.github.avivcarmis</groupId>
    <artifactId>trafficante</artifactId>
    <version>1.0.0</version>
</dependency>
```

To add a dependency on Trafficante Library using Gradle, use the following:
```
compile 'io.github.avivcarmis:trafficante:1.0.0'
```

### Getting Started
------------
Trafficante library divides the server into different `Endpoints`. Each endpoint is responsible to handle requests with certain HTTP method and path (i.e. `POST /get_users`), and defines strongly typed request and response entities. Let's implement some example endpoint:

```
public class GetUserById extends BasicEndpoint<GetUserById.Request, GetUserById.Response, GetUserById.Response> {

    public GetUserById() {
        super(RequestMethod.GET, true);
    }

    @Override
    protected Response handle(Request request) throws APIException {
        Integer userId = request.userId;
        User user = // ...acquire user using userId
        return new Response(user.getFirstName(), user.getLastName(), user,getWebsiteURL());
    }

    @Override
    protected Response wrapResponse(Response response) {
        return response;
    }

    @Override
    protected Response wrapFailure(Throwable throwable) {
        return new Response(null, null, null);
    }

    public static class Request {

        private Integer userId;

    }

    public static class Response {

        private final String firstName;

        private final String lastName;

        private final String websiteURL;

        public Response(String firstName, String lastName, String websiteURL) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.websiteURL = websiteURL;
        }

    }

}
```

So what's going on here? Our endpoint class extends abstract `BasicEndpoint`, declares it's request and response entity to be `Request` and `Response` classes (just ignore the third generic type for the moment). The request and response classes are nestedly defined, but they don't need to be and it's entirely up to your own coding style.
Then it declares the HTTP method it expects and whether or not to log the traffic in the super constructor.
The path of the endpoint is automatically derived from the class name, so the above endpoint will be registered to handle calls to `GET /getUserById` or `GET /get_user_by_id` or any other custom naming strategy you initially define (more on naming strategies in a moment).
Then it defines it's business logic, what actually the endpoint class does to process a request and produce a response, using the implemented `handle` method.
And let's also ignore those `wrapResponse` and `wrapFailure` for the moment.

That's it! We have our first endpoint ready!
Now we need to create a main class and start a new Trafficante server:

```
public class Main {

    public static void main(String[] args) {
        Server.start(
            "com.example",                                      // base package name containing all my endpoint classes
            ServerNamingStrategy.SNAKE_CASE,    // property naming strategy to be used server-wide
            "0.0.0.0",                                                 // host name to be registered - "0.0.0.0" to allow all
            8080,                                                       // port to be used
            true,                                                        // whether or not to enable swagger - should typically be `false` in production environments
            args                                                         // program arguments
        );
    }

}
```
Now our server is up and running.

### Let's Dive Deeper
------------
So we've seen the minimal code required to construct a Trafficante server, now let's explore the recommended setting.
Endpoint classes support response wrapping, to allow client a easy parsing of the response in case of either success and failure, and across the entire server.
Let's, for example, consider this JSON structure to be responded for each and every server request:
```
{
    "success": true,
    "result": {/* any response entity */},
    "error": null
}
```
or in case of failure:
```
{
    "success": false,
    "result": null,
    "error": "string describing the error"
}
```
To achieve this we need to follow a few simple steps:
1. Declare response wrapping entity.
2. Define a standard endpoint class.
3. Define additional error handler.

So first, let's declare our wrapping entity. The Java version of the JSON above can be achieved using:

```
public class APIResponse<T> {

    private final boolean success;

    private final T result;

    private final String error;

    private APIResponse(boolean success, T result, String error) {
        this.success = success;
        this.result = result;
        this.error = error;
    }

    public static <T> APIResponse<T> success(T result) {
        return new APIResponse<>(true, result, null);
    }

    public static <T> APIResponse<T> failure(Throwable t) {
        return new APIResponse<>(false, null, t.getMessage());
    }

}
```

This is simple, next, we can easily define a standard abstract endpoint class to be used across the entire server:

```
abstract public class Endpoint<REQ, RES> extends BasicEndpoint<REQ, RES, APIResponse<RES>> {

    public Endpoint(RequestMethod httpMethod, boolean enableFlowLogging) {
        super(httpMethod, enableFlowLogging);
    }

    @Override
    protected APIResponse<RES> wrapResponse(RES response) {
        return APIResponse.success(response);
    }

    @Override
    protected APIResponse<RES> wrapFailure(Throwable t) {
        return APIResponse.failure(t);
    }

}
```

So as you now can see, the third generic type of the `BasicEndpoint` class expects the type of the response wrapper. Since we previously didn't wrap our response, we just passed the same type twice. In the current case we wire the `APIResponse` class to be generated both on failure and on success of the endpoint.

Lastly, we want to be able to control the response of failure that don't get to reach a specific endpoint class, like 404, or 405 HTTP errors for example. To this end, we need to inherit `BasicErrorHandler` class:

```
public class ErrorHandler extends BasicErrorHandler<APIResponse<?>> {

    @Override
    protected APIResponse<?> wrapFailure(Throwable t) {
        return APIResponse.failure(t);
    }

}
```

Now let's get back to our original endpoint example, and re-implement it using our newly create `Endpoint` class:

```
public class GetUserById extends Endpoint<GetUserById.Request, GetUserById.Response> {

    public GetUserById() {
        super(RequestMethod.GET, true);
    }

    @Override
    protected Response handle(Request request) throws APIException {
        Integer userId = request.userId;
        User user = // ...acquire user using userId
        return new Response(user.getFirstName(), user.getLastName(), user,getWebsiteURL());
    }

    public static class Request {

        private Integer userId;

    }

    public static class Response {

        private final String firstName;

        private final String lastName;

        private final String websiteURL;

        public Response(String firstName, String lastName, String websiteURL) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.websiteURL = websiteURL;
        }

    }

}
```

That's a bit cleaner, but more importantly, now our server responds with the same object structure to each request, allowing the client for easy and intuitive consumption.

In order to read and write HTTP headers and status codes, you may call the thread-safe, static methods:
- `BasicEndpoint.requestHeader(String key)` return the value of the given request header or `null` if not found.
- `BasicEndpoint.responseHeader(String key, String value)` which writes a response header.
- `BasicEndpoint.responseStatusCode(HttpStatus status)` which alters the response code.

This methods may be called from anywhere in the code.

### Validation, Naming and Further Customization
----
In real life, we would want to validate the `GetUserById` request. No need to manually trigger such a request. When the endpoint `handle` method is invoked the request object is already validated.
In the above case we did not specify any validation and so any request will be valid. Trafficante supports a simple  two types of validation approach to cover all your needs:
1. Required fields should be annotated with an `@Required` annotation.
2. Any additional validation is defined using the `Validatable` interface.
Let's, for example, consider a valid request object to specify a `userId` with a positive sign integer. Then our `Request` class should be altered this way:

```
    public static class Request implements Validatable {

        @Required
        private Integer userId;

        @Override
        public void validate() throws BadRequestException {
            if (userId < 0) {
                throw new BadRequestException("user id must be >= 0");
            }
        }

    }
```

The term `Naming` in the context of Trafficante refers to two concepts - method of mapping an endpoint class to path, and the naming of properties in the request and the response.
By default, a server-wide naming strategy is picked up, and then all the magic happens. For the above example using snake case naming convention - a valid request may be `GET /get_user_by_id?user_id=1`, and it's response may be:
```
{
    "success": true,
    "result": {
        "first_name": "john",
        "last_name": "doe",
        "website_url": "example.com"
    },
    "error": null
}
```

The above may be achieved using `ServerNamingStrategy.SNAKE_CASE` property naming when starting a new Trafficante server like in the example above. Trafficante offers 3 basic out of the box strategies to cover the common cases:
1. `ServerNamingStrategy.SNAKE_CASE` which translate naming from camel case to snake case.
2. `ServerNamingStrategy.CAMEL_CASE` which translate naming from camel case to lower camel case. in this case, typically field names remain the same and class names receive a lowercase first character.
3. `ServerNamingStrategy.UNPROCESSED` which doesn't translate names at all.

To use *any* other naming, implement you own `PropertyNamingStrategy`, or preferably, the simpler `PropertyNamingStrategyBase` version.

To set custom name for a specific request or response field, annotate it using:
```
@JsonProperty("custom_external_name")
```

To set custom naming strategy to a request or a response class, annotate it using:
```
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
```

Endpoint may be further customized using the following method overrides:
- `defaultPathProvider` - To be overridden in case path strategy should be changed. This let's you ignore the server naming strategy *and* the class name, and simply return the endpoint path.
- `defaultInvocationWrapper` - To be overridden in case some operations should be performed before and/or after handling the request. For example, measuring execution time, extra logging, etc...
- `defaultParamsRequestConditionProvider`, `defaultHeadersRequestConditionProvider`, `defaultConsumesRequestConditionProvider`, `defaultProducesRequestConditionProvider` which may be further explained [in Spring documentation](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/RequestMappingInfo.html "in Spring documentation").