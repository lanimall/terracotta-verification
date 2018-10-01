# terracotta-verification

## Description

A simple command line utility that can perform CRUD operations against a Terracotta cluster while remaining connected.
You can start multiple clients to check proper cache replication etc...

## Pre-requisite: Download/Install Terracotta

### Terracotta Enterprise (EE)

For Terracotta Enterprise (also known as BigMemory Max), a trial download with time-limited license is available at:
http://www.terracotta.org/downloads/bigmemory-max/

The downloaded package should be of the format: bigmemory-max-4.3.x.x.x.tar.gz
You will also need the trial license that you can also download from the same web page... 
The license file will be a file named "terracotta-license.key".

### Terracotta Open Source

For Terracotta Open Source, go to: http://www.terracotta.org/open-source/
Download the kit under the section "Terracotta Server Open Source Kit" > "Terracotta Server 4.x and Older".
The downloaded package should be of the format: terracotta-4.3.x.tar.gz

### Install and Run Terracotta for Development

In the simplest sense, the installation steps are:

 - Extract the tar.gz package anywhere you want
   - The extract folder will become your TERRACOTTA_HOME (for the rest of this guide)
   - TERRACOTTA_HOME should have the following sub-folders:
     - server
     - apis
     - tools
     - ...
 - (For EE only) Copy the downloaded trial license into $TERRACOTTA_HOME base folder
 - Navigate to $TERRACOTTA_HOME/server/bin/
 - Ensure JAVA_HOME is set to a supported JRE
   - export JAVA_HOME="/path/to/java/home"
 - Optionnaly, add some extra JAVA_OPTS (for example, increase heap memory)
   - export JAVA_OPTS="$JAVA_OPTS -Xms4G -Xmx=4G"
 - To start, run: ./start-tc-server.sh (for Linux / Mac) or start-tc-server.bat (for windows)
 - To stop, run: ./stop-tc-server.sh (for Linux / Mac) or stop-tc-server.bat (for windows)

## Building The App

For Terracotta Open Source 4.3.2:

```bash
mvn clean package -P ehcache
```

For Terracotta EE, use the ehcache-ee profile

```bash
mvn clean package -P ehcache-ee
```

## Running the App

From the same folder where you built the app, add the Terracotta client jars to the classpath, 

```bash
export CLASSPATH_PREFIX=${TERRACOTTA_HOME}/apis/ehcache-ee-terracotta-client-all.jar
```

AND if testing EE, add tge license file property into the JAVA_OPTS:

```bash
export JAVA_OPTS=$JAVA_OPTS -Dcom.tc.productkey.path=$TERRACOTTA_HOME/terracotta-license.key
```

And then run the app.

```bash
sh target/appassembler/bin/Launcher
```

Now, you should see the following self-explanatory menu that you can use to add/remove/list entries in Terracotta.
And if you start multiple app processes (eg. in multiple terminal windows) you can verify that data synchronization between all the processes.

```bash
What do you want to do now?
1 - Load cache with elements (1 <number of elements> <BulkMode=true|false*>)
2 - Display a cache element (2 <key>)
3 - Display all cache elements (3 <ExpiryCheck=true|false*> <DoNotDisplayEach=true|false*>)
4 - Query the cache with SQL (4 <sql>)
5 - Check if key is in cache (5 <key> <ExpiryCheck=true|false*>)
6 - Add new element to cache (6 <username>)
7 - Update an existing cache element (7 <key to update> <new username>)
8 - Delete a cache element by key (8 <key to delete>)
9 - Remove all cache entries
10 - Display cache size
11 - Quit program
>>
```