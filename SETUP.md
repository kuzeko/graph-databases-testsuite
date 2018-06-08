# Graph Database Setup

To include the docker image for a new GDB system one needs to prepare the Docker Image file, the init-scripts, checks properties, and then the queries.
It is encouraged to see other database and how they are set up when including a new one in the framework.

In the following we outline the main steps. It is assumed the user is in the root of the folder.

Typing the query language in the files are optional, but gives other users a better idea of which files to use when testing.

1. Create a .dockerfile in the `images/` folder that follows the naming
    `gremlin-<name-of-db>-<version>-<query-language>.dockerfile`

2. External files required for import by the Docker image can be put into `images/extra` with prefix (e.g., additions or new `.groovy` files, settings for embedded databases):
    `<name-of-db>-<version>-<query-language>-<filename-with-extension>`

3. Add an `init.sh` in `images/init/` that should be called as the last thing at the end of the Docker image:
   `<name-of-db>-<version>-<query-language>-init.sh`
    This is supposed to make sure the environment is set up correctly, appending specific loaders etc. from the `images/extra/`.  

4. Configure imports for the gremlin console in `/runtime/tp<TinkerPop-version>/headers.groovy.sh`:  
   Commands on the same line should be seperated by `;`. This may not work all the times, and it suggested to insert `\n` after each semi-colon should the issue arise:
   `[<name-of-db>-<query-language>] = "<import-statement>",`

5. Configure initialisation commands for the gremlin console in `/runtime/tp<TinkerPop-version>/headers.groovy.sh`.
   Commands should be seperated by `;`:
   `[gremlin-<name-of-db>-<query-language>] = "<commands>",`

6. Add database and image name in `test.py` in the corresponding arrays:
	`<database-name>` & `<dbtrento/gremlin-<database-name>`

* More details on how to use the test suite: [RUN.md](RUN.md)

* Insights on the folder structure and scripts role: [FILES.md](FILES.md)
