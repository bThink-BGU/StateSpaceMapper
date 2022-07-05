[![](https://jitpack.io/v/bThink-BGU/StateSpaceMapper.svg)](https://jitpack.io/#bThink-BGU/StateSpaceMapper)

# StateSpaceMapper
A utility for mapping state spaces of b-programs written in BPjs.

The utility makes use of [BPjs](https://github.com/bThink-BGU/BPjs), [JGraphT](https://jgrapht.org/), and [GOAL](http://goal.im.ntu.edu.tw/).

## Running from JAR
1. Download JAR for the [latest version](https://github.com/bThink-BGU/StateSpaceMapper/releases/latest).
2. Create a js file in and add your b-thread to the file.
3. Run:
```
java -jar <path-to-download-jar> "path-to-your-js-file(s)"
```

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
mvn exec:java -D"exec.args"="path-to-your-js-file(s)"
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
		<version>0.4.7</version>
	</dependency>
</dependencies>
```

## Usage
There are several usage examples, in: 
* [SpaceMapperCliRunner.java](src/main/java/il/ac/bgu/cs/bp/statespacemapper/SpaceMapperCliRunner.java)
* [LevelCrossingMain.java](src/test/java/il/ac/bgu/cs/bp/statespacemapper/levelCrossing/LevelCrossingMain.java)

Once the run is completed, a new directory, called "exports", will be created, with the output files inside.

## Accepting states
In your js code, you may mark certain states as accepting by using the following code:
```javascript
if(typeof use_accepting_states !== 'undefined') {
  AcceptingState.Stopping() // or AcceptingState.Continuing()
}
```

The ```if(typeof use_accepting_states !== 'undefined')``` condition will allow you to use the same code both in BProgramRunner and in StateSpaceMapper.

The ```AcceptingState.Stopping()``` will cause the StateMapper to stop the mapping for this branch and mark the state as accepting. The StateMapper **will** continue the state mapping in other branches.

The ```AcceptingState.Continuing()``` will mark the state as accepting, without stopping the mapping for this branch.
This type of accepting state is useful for Büchi automata, that accepts an input iff there is a run of the automaton over the input that begins at an initial state and  at least one of the infinitely often occurring states is an accepting state.

## Export formats
Currently, the supported formats are:
* [JSON](https://jgrapht.org/javadoc/org.jgrapht.io/org/jgrapht/nio/json/JSONExporter.html)
* [DOT (GraphViz)](https://graphviz.org/) (default)
* [GOAL](http://goal.im.ntu.edu.tw) - a graphical interactive tool for defining and manipulating Büchi automata and temporal logic formulae.

[comment]: <> (* [Noam]&#40;https://github.com/izuzak/noam&#41; &#40;allows for translating the automaton into a regular expression&#41;)

[comment]: <> (* [Regular Expression]&#40;http://goal.im.ntu.edu.tw&#41; - Uses GOAl to translate the automaton into a regular expression&#41;)

[comment]: <> (* [Neo4J]&#40;https://neo4j.com/&#41; &#40;requires an installation of Neo4J and configuring the driver ```mpr.setNeo4jDriver&#40;driver&#41;;```&#41;)

### Manipulating current exporter
Current exporter can be easily manipulated without extending them. See the setters of the [Exporter](src/main/java/il/ac/bgu/cs/bp/statespacemapper/jgrapht/exports/Exporter.java) class.
These setters allow for manipulating the vertex, edge, and graph attributes. 
Additionally, they allow for changing the String sanitizer, to replace special exporter characters. 

### Adding and extending the Output formats
You can create your own output format by extending the [Exporter](src/main/java/il/ac/bgu/cs/bp/statespacemapper/jgrapht/exports/Exporter.java) class. 
If jgrapht support this format - you can follow the example of [DotExporter](src/main/java/il/ac/bgu/cs/bp/statespacemapper/jgrapht/exports/DotExporter.java). 
Otherwise, you can follow the example of [GoalExporter](src/main/java/il/ac/bgu/cs/bp/statespacemapper/jgrapht/exports/GoalExporter.java).

The writers can configure the format/text of each node and edge, and to define the overall format of the output file. 

## Getting all possible traces
You can generate a set of all possible traces, see [SpaceMapperCliRunner.java](src/main/java/il/ac/bgu/cs/bp/statespacemapper/SpaceMapperCliRunner.java). 

**Warning:** the list of all possible traces may grow exponentially in the graph depth. Consider limiting the maximum traces length.

## Per-BThread StateSpaceMapper
Since version 0.4.0, it is possible to generate a forest of b-threads state-spaces (i.e., one state-space for each b-thread).

:exclamation: To use this feature, the following conditions must be met:  

1. The b-program must follow the following structure:
```javascript
const bthreads = {}
bthreads['<first b-thread name>'] = function () { /** b-thread body **/}
bthreads['<second b-thread name>'] = function () { /** b-thread body **/}
// [other bthreads...]

// Do not start these b-threads, they are started externaly.
// No additional code except for const variables.
```
2. `waitFor`, `block`, and `interrupt`, may accept only a `BEvent` or an array of `BEvent`.

The trick behind this execution is the conversion of `waitFor` events to `request` events at the `EventSelectionStrategy`.

### Running the Per-BThread StateSpaceMapper
* [PerBTSpaceMapperRunner.java](src/main/java/il/ac/bgu/cs/bp/statespacemapper/PerBTSpaceMapperRunner.java)