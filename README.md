# Smart Campus Sensor & Room Management API

## Overview

This project is a RESTful API built using **JAX-RS (Jersey)** and an embedded **Grizzly HTTP server**,
developed as part of the 5COSC022W Client-Server Architectures coursework at the University of Westminster.

The API simulates a "Smart Campus" infrastructure management system that allows campus facilities
managers to manage **Rooms** and **Sensors** deployed across the university. It supports full CRUD
operations, sensor reading history, filtered queries, custom error handling, and request/response logging.

### Key Features
- Room management (create, retrieve, delete with safety constraints)
- Sensor management (create, filter by type, link to rooms)
- Sensor reading history with automatic current value updates
- Custom exception mappers for meaningful error responses
- Global catch-all error handler to prevent stack trace leaks
- Request and response logging via JAX-RS filters

### Resource Hierarchy
```
|--- /api/v1 
    |--- /rooms 
        |--- GET - List all rooms 
        |--- POST - Create a new room 
        |--- /{roomId} 
            |--- GET - Get room by ID 
            |--- DELETE - Delete room (only if no sensors assigned) 
    |--- /sensors 
        |--- GET - List all sensors (optional ?type= filter) 
        |--- POST - Register a new sensor 
        |--- /{sensorId}/readings 
            |--- GET - Get reading history for a sensor 
            |--- POST - Add a new reading for a sensor
```

---

## Technology Stack

- **Java 11**
- **JAX-RS (Jakarta RESTful Web Services)**
- **Jersey 3.1.3** (JAX-RS implementation)
- **Grizzly HTTP Server** (embedded, no Tomcat needed)
- **Jackson** (JSON serialization)
- **Maven** (build tool)

---

## How to Build and Run the Project

### Prerequisites
Make sure you have the following installed:
- Java JDK 11 or higher → https://www.oracle.com/java/technologies/downloads/
- Maven 3.6 or higher → https://maven.apache.org/download.cgi
- Git → https://git-scm.com/downloads

You can verify installations by running:
```bash
java -version
mvn -version
git –version
```

Step 1 — Clone the Repository
```bash
git clone https://github.com/YOUR_GITHUB_USERNAME/smart-campus-api.git
cd smart-campus-api
```

Step 2 — Build the Project
```bash
mvn clean package
```
This will download all dependencies and package the project into a single runnable JAR file located at target/smart-campus-api-1.0-SNAPSHOT.jar.

Step 3 — Run the Server
```bash
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```
You should see:
Smart Campus API running at http://localhost:8080/api/v1
Press ENTER to stop the server...

Step 4 — Verify it's running
Open your browser and go to:
http://localhost:8080/api/v1
You should see a JSON response with API metadata.

________________________________________
Sample curl Commands

1. Discovery — Get API metadata
```bash
curl -X GET http://localhost:8080/api/v1
```
Expected response:
```json
{
  "version": "1.0",
  "description": "Smart Campus Sensor & Room Management API",
  "contact": "admin@smartcampus.com",
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "LIB-301",
    "name": "Library Quiet Study",
    "capacity": 50
  }'
```
Expected response (201 Created):
```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 50,
  "sensorIds": []
}
```

3. Create a Sensor linked to a Room
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.5,
    "roomId": "LIB-301"
  }'
```
Expected response (201 Created):
```json
{
  "id": "TEMP-001",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 22.5,
  "roomId": "LIB-301"
}
```

4. Get all Sensors filtered by type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"
```
Expected response:
```json
[
  {
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.5,
    "roomId": "LIB-301"
  }
]
```

5. Add a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 24.3}'
```
Expected response (201 Created):
```json
{
  "id": "some-uuid",
  "timestamp": 1713960000000,
  "value": 24.3
}
```

6. Get Reading History for a Sensor
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

7. Delete a Room (will fail if sensors are assigned)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```
Expected error response if sensors exist (409 Conflict):
```json
{
  "error": "Conflict",
  "message": "Room still has active sensors assigned to it."
}
```

________________________________________
Report — Answers to Coursework Questions
Part 1: Service Architecture & Setup 

1. Project & Application Configuration :

- Question: In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions. 
- Answer:
By default JAX-RS creates a new instance of a resource class for each incoming request (per-request lifecycle). This implies that every single HTTP request is assigned a new object, preventing thread-safety issues on an instance level.
But this poses a serious problem in handling in-memory data structures. Because a resource instance is created each time a request is made, the data cannot be stored as instance variables, because they would be lost every time a request is made. To address this, shared data must be stored in a static and centralized store such as DataStore class with static HashMaps.
Race conditions become a real concern since multiple requests may access the static maps simultaniously. ConcurrentHashMap would be used instead of HashMap, or add synchronized blocks to stop two threads from modifying the data simultaneously, which could lead to data loss or corruption.

2. The ”Discovery” Endpoint :

- Question: Why is the provision of ”Hypermedia” (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation? 
- Answer:
HATEOAS (Hypermedia as the Engine of Application State) is the fact that API responses are not only a list of raw data, but also links to related actions and resources. As an example, when you fetch a room, the response include links like delete room or view its sensors.
This will help the client developers in multiple ways compared to the static documentation. First, the clients can dynamically navigate the API without any hardcoded URLs, So that if the API changes its URL structure, links which are followed by the clients will still continue to be functional. Second, it makes the API self-descriptive. For instance a developer can explore it by just following the links in responses, like browsing a website. Third, it reduces coupling between the client and the server, as the client does not need to be aware of the complete URL structure initially. Static documentation can be outdated, but hypermedia responses are always current.

Part 2: Room Management

1. RoomResource Implementation :

- Question: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing. 
- Answer:
The response can be very lightweight by returning just IDs, which reduces network bandwidth and speeds up the response. But then the client will have to make separate requests per each room to obtain its details, which results in the N+1 problem, one request to get the IDs, and then N additional requests to get the information of each room. This increases total latency significantly.
Returning full room object increase the response size, but the client receives everything in a one single request, reducing round trips. In case if a campus has thousands of rooms, sending full objects can be heavy on bandwidth. Returning a summary object per room like just id, name, capacity instead of the entire object with all the nested sensor IDs, is a good middle ground.

2. RoomDeletion & Safety Logic :

- Question: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.  
- Answer:
Yes, DELETE is idempotent in this implementation. Idempotency means that making the same request multiple times to the server result in the same state of the server as making a single request.
In our implementation, the first DELETE request on a valid room removes it from the DataStore and returns 204 No Content. If the client sends the same DELETE request again, that room is no longer available therefore the service returns 404 Not Found. The server state does not change and the room is still gone. There are no other side effects. This is consistent with the REST standards, where idempotency is the state of the resource, and not necessarily the response code.



Part 3: Sensor Operations & Linking 

1. Sensor Resource & Integrity :

- Question: We explicitly use the @Consumes (MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch? 
- Answer:
The @Consumes(MediaType.APPLICATION_JSON) annotation tells JAX-RS that this endpoint only accepts JSON. And when a client makes a request with a Content-Type of text/plain or application/xml, JAX-RS will automatically reject the request before it even reaches to the method. It returns HTTP 415 Unsupported Media Type response, indicating that the server cannot process the format that the client has sent. This is completely handled by the JAX-RS framework, thus no manual checking is required within the method.

2. Filtered Retrieval & Search :

- Question: You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections? 
- Answer:
Using @QueryParam (e.g., GET /api/v1/sensors?type=CO2) is generally considered superior to path-based filtering (e.g., GET /api/v1/sensors/type/CO2) due to several reasons.
Query parameters are semantically correct. It represents optional filters on a collection, not a separate resource. A path segment such as /sensors/type/CO2 suggests that type/CO2 is a particular resource, which is misleading. The query parameters are also more flexible and scalable. It is easy to add multiple filters such as ?type=CO2&status=ACTIVE without changing the URL structure. In a case of a path-based filtering, the addition of each additional filter would create more complicated and unreadable URL patterns. Also, query parameters are the REST standard of searching and filtering collections, which makes the API easier to be used by other developers.




Part 4: Deep Nesting with Sub - Resources 

1. The Sub-Resource Locator Pattern :

- Question: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive con troller class? 
- Answer:
The Sub-Resource Locator pattern enhances API design by separating concerns into specific classes. Instead of a massive resource class containing all logic for sensors, rooms, and readings, each resource has its own focused class with a single responsibility.
This simplifies the codebase greatly in terms of maintenance and extension. If how readings work is needed to be changed, only SensorReadingResource has to be edited without risking breaking the sensor or room logic.  It also enhances readability. For example, a developer who is new to the project can quickly figure out what each class is for. In large APIs that have dozens of nested resources, one controller class would become thousands of lines long and practically unmanageable. The locator pattern enables reusability, as the sub-resource class can be used in multiple contexts.


Part 5: Advanced Error Handling, Exception Mapping & Logging 

1. Dependency Validation (422 Unprocessable Entity) :

- Question: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload? 
- Answer:
The traditional meaning of a 404 Not Found is the requested URL or the resource doesn’t exist on the server. But, in this case the URL (/api/v1/sensors) is correct and valid, the issue is that the data inside the request body is referring to a roomId which does not exist.
The HTTP 422 Unprocessable Entity is more semantically correct since it indicates the server understood the request and its format but could not process it as it had a logical or business rule violation in the content. This request was syntactically correct JSON, but semantically invalid as it was a request to a resource that did not exist. In this case, 404 would be misleading. It would mean that the endpoint itself is not found, confusing client developers who are debugging the problem.


2. The Global Safety Net :

- Question: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace? 
- Answer:
Exposing raw Java stack traces in API error responses is a serious information disclosure vulnerability. An attacker can gather several types of sensitive information from a stack trace. They can identify the exact library versions and frameworks being used, which allows them to look up known vulnerabilities for those specific versions. They are also able to view the internal package and class hierarchy of your application and can more easily deploy targeted attacks. Traces can reveal server directory structure by providing file paths. Logic errors revealed in traces can hint at how to create malicious inputs to exploit them further.
The Global Exception Mapper addresses this by ensuring that all unforeseen errors are caught and only a generic, safe message, “An unexpected error occurred” is returned with no internal information available so that an attacker remains unaware of the implementation details.

3. API Request & Response Logging Filters :

- Question: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?
- Answer:
Hand-inserting Logger.info() within each resource method is not only cause redundancy, but also error prone. a developer could easily forget to add logging to a new endpoint. JAX-RS filters implement logging as a cross-cutting concern, meaning it applies automatically to each and every request and response without touching individual resource classes.
This approach is also much easier to maintain. For example if the log format needs to be changed, it can be changed in one place (the filter) rather than searching through all the methods. Filters also have access to low-level request/response context that individual methods don't naturally have, like headers and status codes. This separation makes the resource classes clean and only based on business logic, following the Single Responsibility Principle.

