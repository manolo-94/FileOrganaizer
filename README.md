# Manual de Empaquetado y Ejecución de un Proyecto JavaFX con Maven en IntelliJ IDEA

## Descripción del Proyecto
Este proyecto permite organizar archivos en sus carpetas respectivas de forma automática. Cada archivo debe tener el mismo nombre que la carpeta de destino para ser ubicado correctamente.

El usuario solo necesita seleccionar:
1. La ubicación de las carpetas de destino.
2. La carpeta donde se encuentran los archivos a organizar.

El sistema procesará los archivos y los moverá a sus respectivas carpetas según su nombre.

## Requisitos Previos
- IntelliJ IDEA instalado.
- Java 17.0.11 OpenJDK con soporte para JavaFX.
- Maven configurado en el sistema.

## Creación del Proyecto
1. Abrir IntelliJ IDEA y seleccionar **Nuevo Proyecto**.
2. Elegir **JavaFX** como tipo de proyecto.
3. Seleccionar **Java** como lenguaje y **Maven** como sistema de construcción.
4. Usar la versión **17.0.11 OpenJDK**.
5. Configurar el nombre del proyecto y la ubicación.
6. Hacer clic en **Finalizar**.

## Estructura del Proyecto
- **Launcher.java** (Ubicada en el paquete principal): Es la clase que inicia la aplicación.
- **AppApplication.java**: Contiene la lógica principal de organización de archivos.

## Configuración del `pom.xml`
Editar el archivo `pom.xml` para agregar y configurar el plugin `maven-assembly-plugin`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>3.4.0</version>
            <configuration>
                <descriptorRefs>
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
            </configuration>
            <executions>
                <execution>
                    <id>make-assembly</id>
                    <phase>package</phase>
                    <goals>
                        <goal>single</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Construcción del Proyecto
1. Abrir la pestaña de **Maven** en IntelliJ IDEA.
2. Hacer clic en el botón de **sincronización** para asegurarse de que Maven descargue las dependencias correctamente.
3. Expandir la sección **Lifecycle** en la pestaña de Maven.
4. Hacer doble clic en la opción **package**.
5. Esperar a que finalice el proceso y verificar que se generó la carpeta `target`.

## Ubicación del JAR Ejecutable
Dentro de la carpeta `target`, se generará un archivo con un nombre similar a:

```
nombre-de-la-app-SNAPSHOT-jar-with-dependencies.jar
```

## Ejecución del JAR
### Desde Finder en macOS:
1. Navegar hasta la carpeta `target` en Finder.
2. Hacer clic derecho sobre el archivo `jar-with-dependencies.jar` y seleccionar **Abrir con -> Finder**.
3. Hacer doble clic sobre el archivo para ejecutarlo.

### Desde la Consola:
Abrir la terminal y ejecutar:

```sh
java -jar target/nombre-de-la-app-SNAPSHOT-jar-with-dependencies.jar
```

## Requisitos en la Máquina de Ejecución
Para ejecutar el JAR en otra máquina, se debe tener instalado:
- Java 17.0.11 OpenJDK.
- JavaFX configurado en la misma versión.

Con estos pasos, podrás empaquetar y ejecutar tu proyecto JavaFX en cualquier entorno compatible con Java 17 y JavaFX.

