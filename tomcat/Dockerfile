
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copy the project definition first (for better caching)
COPY pom.xml .

# 2. Copy  source code

COPY src ./src

# 3. Build the WAR file

RUN mvn clean package -DskipTests

# --- Stage 2: Run the Application (Tomcat) ---

FROM tomcat:10.1-jdk17
WORKDIR /usr/local/tomcat/webapps/


RUN rm -rf ROOT examples docs

# 2. Copy the WAR file from the 'build' stage

COPY --from=build /app/target/bookstore.war ./ROOT.war

# 3. Expose the web port
EXPOSE 8080

# 4. Start Tomcat
CMD ["catalina.sh", "run"]
