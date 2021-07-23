[![](https://jitpack.io/v/bThink-BGU/StateSpaceMapper.svg)](https://jitpack.io/#bThink-BGU/StateSpaceMapper)

# StateSpaceMapper
A utility for mapping state spaces of b-programs written in BPjs.

## Running from sources
1. Clone the project and compile it:
```
git clone https://github.com/bThink-BGU/StateSpaceMapper.git
cd StateSpaceMapper
mvn compile
```
2. Create a js file in and add your b-thread to the file.
3. Run:
```
mvn exec:java -D"exec.args"="path-to-your-js-file"
```

## Embedding StateSpaceMapper 
Create a Maven project and add the followings to your pom.xml:
```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
```
```xml
<dependencies>
	<dependency>
		<groupId>com.github.bThink-BGU</groupId>
		<artifactId>StateSpaceMapper</artifactId>
		<version>0.2.12</version>
	</dependency>
</dependencies>
```

## Usage
After creating BProgram bprog, write the following code (replace "vault" with your file name):
```java
BProgram bprog = new ResourceBProgram("vault.js");
StateSpaceMapper mpr = new StateSpaceMapper("vault");
mpr.mapSpace(bprog);
```
Once the run is completed, a new directory, called "graphs", will be created, with the output files inside.

## Configuration
You can change the default output directory by calling: ```mpr.setOutputPath("graphs");```

You can generate a set of all possible traces, by calling ```mpr.setGenerateTraces(true);``` (default=true)

## Accepting states
In your js code, you may mark certain states as accepting by using the following code:
```javascript
if(use_accepting_states) {
  AcceptingState.Stopping() // or AcceptingState.Continuing()
}
```

The ```if(use_accepting_states)``` condition will allow you to use the same code both in BProgramRunner and in StateSpaceMapper. 

The ```AcceptingState.Stopping()``` will cause the StateMapper to stop the mapping for this branch and mark the state as accepting. The StateMapper **will** continue the state mapping in other branches.

The ```AcceptingState.Continuing()``` will mark the state as accepting, without stopping the mapping for this branch. 
This type of accepting state is useful for Buchi automatons, that accepts an input iff there is a run of the automaton over the input that begins at an initial state and  at least one of the infinitely often occurring states is an accepting state.

## Output formats
Currently, the supported formats are:
* Json
* [Noam](https://github.com/izuzak/noam) (allows for translating the automaton into a regular expression)
* [GraphViz](https://graphviz.org/)
* [GOAL](http://goal.im.ntu.edu.tw) - a graphical interactive tool for defining and manipulating Büchi automata and temporal logic formulae.
* [Regular Expression](http://goal.im.ntu.edu.tw) - Uses GOAl to translate the automaton into a regular expression)
* [Neo4J](https://neo4j.com/) (requires an installation of Neo4J and configuring the driver ```mpr.setNeo4jDriver(driver);```)

### Adding and extending the Output formats
You can create your own output format by extending the TraceResultWriter class. See current writers for examples.

You can also extend any of the current output formats.

The writers can configure the format/text of each node and edge, and to define the overall format of the output file. 
