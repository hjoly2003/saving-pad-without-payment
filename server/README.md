# Saving pads without payment

## Requirements

- Maven
- Java

1. Build the jar
   ```
   mvn package
   ```

2. Run the packaged jar
   ```
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=localhost:5005 -cp target/sample-jar-with-dependencies.jar com.stripe.sample.Server
   ```

3. Go to `localhost:4242` in your browser to see the demo

Note: To clear the content of a Windows terminal, type:
    ```
    cls
    ```

Use the [EpochConverter online](https://www.epochconverter.com/)