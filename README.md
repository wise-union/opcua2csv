# OPC UA Client Example

This project is used to export node information from an OPC UA server, and save into a csv file.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

- Java 1.8 or higher
- Maven

### Installing

1. Clone the repository
```bash
git clone https://github.com/wise-union/opcua2csv.git
```

2. Navigate to the project directory
```bash
cd opcua2csv
```

3. Build the project using Maven
```bash
mvn clean install
```

## Running the Application

To run the application, use the following command:

```bash
java -jar Opcua2Csv-1.0-SNAPSHOT-jar-with-dependencies.jar opc.tcp://LAPTOP-VI4T7MK9:48020 1
```
The first parameter is the opcua server endpoint url.
The second parameter indicates whether export variable node only. (1: only variable nodes will be exported; 0: all nodes will be exported)

## Built With

* [Eclipse Milo](https://github.com/eclipse/milo) - An open-source implementation of OPC UA
* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Wise Union**

## License

This project is licensed under the MIT - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Thanks to the Eclipse Milo authors for providing the base code for this project.
