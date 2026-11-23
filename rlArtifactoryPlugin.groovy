// groovylint-disable UnnecessaryGroovyImport
// groovylint-disable CompileStatic
/*
Artifactory 7.xxx
    java: Temurin-21.0.9+10

Artifactory: 7.125
    /opt/jfrog/artifactory/app/artifactory/tomcat/webapps/artifactory/WEB-INF/lib/groovy-4.0.27.jar

Artifactory: 7.133
    /opt/jfrog/artifactory/app/artifactory/tomcat/webapps/artifactory/WEB-INF/lib/groovy-4.0.29.jar
*/
import groovy.transform.Field
import groovy.transform.CompileStatic /* groovylint-disable UnnecessaryGroovyImport */
//
import org.artifactory.fs.FileInfo
import org.artifactory.repo.RepoPath
import org.artifactory.request.Request
import org.artifactory.exception.CancelException

// groovylint-disable UnnecessaryGroovyImport
// groovylint-disable IfStatementBraces
// groovylint disable DuplicateNumberLiteral
// groovylint disable DuplicateStringLiteral

import org.slf4j.Logger
//
//
import org.artifactory.repo.Repositories
import org.artifactory.repo.RepositoryConfiguration
import org.artifactory.md.Properties as AfProperties
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException
//
import org.reversinglabs.securesoftwareapi.MyConfig
import org.reversinglabs.securesoftwareapi.RlCommunityProcessor
import org.reversinglabs.securesoftwareapi.BlockAnalizer
import org.reversinglabs.securesoftwareapi.MavenFiles
import org.reversinglabs.securesoftwareapi.Verdict
import org.reversinglabs.securesoftwareapi.NativeCopies
import org.reversinglabs.securesoftwareapi.StackTraceAsStringTrait

///////////////////////////////////////////////////////////////////
// classes
@CompileStatic
class RlArtifactoryAdapter implements StackTraceAsStringTrait { // groovylint-disable ParameterCount

    // args
    Logger logger
    String what = ''
    RepoPath repoPath
    Repositories repositories
    String clientAddress = ''
    String homeDir = ''
    String myPropertiesPath = ''
    String userAgent = ''
    String namespace

    // other (instances)
    MyConfig myConfig
    RlCommunityProcessor rcp
    BlockAnalizer ba
    AfProperties myProperties //af
    RepositoryConfiguration repoConfig // af

    String repoKey = ''
    String path = ''
    String rType = ''
    String pType = ''
    String sha256  = ''
    boolean file = false
    String[] propertyKeys = []
    Map props = [:] // filled by: currentPropertiesAsMap()

    int verbose = 0

    Map originPropsClean = [:]
    boolean haveCommunity = false

    RlArtifactoryAdapter(
        Logger logger,
        int verbose,
        String what,
        RepoPath repoPath,
        Repositories repositories,
        String clientAddress,
        String homeDir,
        String myPropertiesPath,
        String userAgent,
        String namespace = 'secure.software'
    ) {
        // args
        this.logger = logger
        this.verbose = verbose
        this.what = what
        this.repoPath = repoPath
        this.repositories = repositories
        this.clientAddress = clientAddress
        this.homeDir = homeDir
        this.myPropertiesPath = myPropertiesPath
        this.userAgent = userAgent
        this.namespace = namespace
        // other
        this.initMe()
    }

    Map rlBlockVerdictOnDownload() {
        /* we load the config from <plugin.properies> in the <plugin directory>.
         * we fetch the current properties for this item from artifactory.
         * we do a community lookup and update the properties with info from the community.
         * we analize the result status from artifactory
         *   and the optional existing properties from a rl-scan-artifactory run
         *   or a forced no block baed on cloent-ipaddress or repo or no block at all.
         */
        String mName = 'rlBlockVerdictOnDownload'
        String context = "${this.what}::${mName}"
        if (this.verbose) this.logger.info '{}:' , context // groovylint-disable DuplicateStringLiteral

        // String path = this.repoPath.toPath() ?: ''

        // get the current config
        if (this.loadConfig() == false) { return [:] }

        // get the current file properties from artifactory
        Map props1 = this.myArtifactoryProps()
        if (this.verbose) this.logger.info '{}: artifactoryProps: {}' , context, props1
        if (props1.size() == 0) { return [:] } // groovylint-disable DuplicateNumberLiteral

        // cleanup the props and remove all with RL. and <namespace> any left over will not be changed
        this.originPropsClean = [:]
        for (item in props1) {
            String key = item.key
            String value = item.value
            if (key.startsWith(this.namespace)) {
                continue
            }
            if (key.startsWith('RL.')) {
                continue
            }
            this.originPropsClean[key] = value
        }
        if (this.verbose) this.logger.info '{}: originPropsClean: {}' , context, this.originPropsClean

        // do community
        Map props2 = this.doReversingLabsCommunity(props1)
        if (this.verbose) this.logger.info '{}: ReversingLabs CommunityProps: {}' , context, props2
        if (props2.size() == 0) { return [:] }  // groovylint-disable DuplicateNumberLiteral

        // evaluate the results
        Map result = this.doBlockAnalizer(props2)
        if (this.verbose) this.logger.info '{}: BlockAnalizer Result:: {}' , context, result
        return result
    }

    private Object initMe() {
        String mName = 'initMe'
        String context = "${this.what}::${mName}"
        if (this.verbose) this.logger.info '{}:' , context // groovylint-disable DuplicateStringLiteral

        try {
            this.repoKey = this.repoPath.repoKey // the artifactName basically
            this.path = this.repoPath.toPath() ?: '' // the artifact path
            this.myProperties = this.repositories.getProperties(this.repoPath)
            this.propertyKeys = this.myProperties.keys() // keys() is a af thing

            this.repoConfig = this.repositories.getRepositoryConfiguration(this.repoPath.repoKey)
            this.rType = this.repoConfig.type // local, remote, cache, ...
            this.pType = this.repoConfig.packageType // pypi, gem, npm , ...
            this.file = this.repoPath.isFile() //  bool
        } catch (e) {
            printTrace(mName, e)
            this.logger.error '{}: {}', context, e.toString(), e
        }
    }

    private boolean loadConfig() {
        String mName = 'loadConfig'
        String context = "${this.what}::${mName}"
        if (this.verbose) this.logger.info '{}:' , context // groovylint-disable DuplicateStringLiteral

        // load the config
        try {
            this.myConfig = new MyConfig(
                this.logger,
                this.verbose,
                "${this.homeDir}/${this.myPropertiesPath}",
                "${this.homeDir}/plugins",
                this.userAgent,
            )
            this.verbose = this.myConfig.verbose
        } catch (e) {
            printTrace(mName, e)
            this.logger.error '{}: myConfig Failed: {}', context, e.toString(), e
            return false
        }
        return true
    }

    private Map myArtifactoryProps() {
        String mName = 'myArtifactoryProps'
        String context = "${this.what}::${mName}"
        if (this.verbose) this.logger.info '{}:' , context // groovylint-disable DuplicateStringLiteral

        Map props1 = [:]

        if (this.file == false) {
            return props1
        }

        try {
            // populate the sha256 which we will need for looking up the item in the community
            this.sha256 = this.myFileInfo()

            // get the current artifactory props
            props1 = this.currentPropertiesAsMap()
            this.logger.debug '{}: sha256: {}' , context, this.sha256
            return props1
        } catch (e) {
            printTrace(mName, e)
            this.logger.error '{}: Failed: {}', context, e.toString(), e
        }

        return [:]
    }

    private String myFileInfo() {
        String mName = 'myFileInfo'
        String context = "${this.what}::${mName}"
        if (this.verbose) this.logger.info '{}:' , context // groovylint-disable DuplicateStringLiteral

        try {
            // Note: may not be present if there is no file yet: fileInfo sha256
            FileInfo fileInfo = repositories.getFileInfo(this.repoPath)
            String sha256 = fileInfo.getSha2() // groovylint-disable UnnecessaryGetter
            if (this.verbose) this.logger.info "{}: {} sha2: '{}'",  context, fileInfo, sha256
            this.sha256 = sha256
            return sha256
        } catch (ItemNotFoundRuntimeException e) {
            this.logger.warn '{}: no sha256 yet', context
        } catch (e) {
            printTrace(mName, e)
            this.logger.error '{}: no fileInfo (sha256) here {}', context, e.toString(), e
        }
        return ''
    }

    private Map doReversingLabsCommunity(Map props1) {
        /* With the given props Map,
         *   we call the Community processor that finally calls ApplyOverride.
         * Along the way new props are build from community that we can apply back to artifactory,
         *   to leave evidence the info from the reversinglabs community.
         * We then update artifactory with the new props,
         *   and refetch the now actual props and return them to the caller.
         */
        String mName = 'doReversingLabsCommunity'
        String context = "${this.what}::${mName}"
        if (this.verbose) this.logger.info '{}: props1: {}' , context , props1

        Map props2 = [:]
        // call the ReversingLabs Community processor
        try {
            if (this.verbose) this.logger.info '{}: server: {}' , context , this.myConfig.community.server
            // Q22 (2.2.0): in a Maven (2.3.0: or Gradle) repo only binaries (.jar/.war/.ear/.aar) are looked up
            boolean inspectFile = MavenFiles.shouldInspect(this.pType, this.path)
            if (!inspectFile && this.verbose) {
                this.logger.info '{}: {} repo, no lookup for {}', context, this.pType, this.path
            }
            if (this.myConfig.community.server && inspectFile) {
                this.rcp = new RlCommunityProcessor(
                    this.logger,
                    this.verbose,
                    context + '::RlCommunityProcessor',
                    this.myConfig,
                    this.file,
                    this.pType,
                    this.sha256,
                    props1,
                )
                // -------------------------------
                Map result = this.rcp.run()
                if (this.verbose) this.logger.info '{}: result from run: {}' , context , result

                // -------------------------------
                this.haveCommunity = false
                if (result.status == true) {
                    this.haveCommunity = true
                }
                if (this.verbose) this.logger.info '{}: haveCommunity: {}' , context , this.haveCommunity

                // -------------------------------
                // currentProps start
                // Q28 (2.3.4): only new community data is written. Without it, myProps() returns all stored
                // properties, and writing those back copied the native ones (npm.name, ...) into secure.software.*
                Map currentProps = rcp.newProps ? new LinkedHashMap(rcp.newProps) : [:]
                if (this.verbose) this.logger.info '{}: currentProps: {}' , context , currentProps

                // -------------------------------
                // propagate the result to the props
                Map rData = result.data as Map ?: [:]
                for (item in rData) {
                    currentProps[item.key] = item.value
                }

                // -------------------------------
                // Q21: only (re)compute the verdict when this event produced new community data: those props
                // have plain keys ('status'). Without new data, myProps() returns the stored Artifactory
                // properties (namespaced: 'secure.software.status'); then the stored verdict stays as it is.
                // Before, 'status' was missing there and secure.software.verdict was overwritten with 'null'.
                if (currentProps.status != null) {
                    // SPEC-4.5: the verdict rule lives in the jar (Verdict.of, 2.3.3), unit-tested offline
                    currentProps.verdict = Verdict.of(currentProps.status, currentProps.removed, currentProps.is_quarantined)
                } else {
                    currentProps.remove('verdict') // never write a computed 'null'
                }
                if (this.verbose) this.logger.info '{}: currentProps: {}' , context , currentProps

                this.doNewProps(currentProps) // update artifactory with current props
            // currentProps end
            }

            // refetch the props from artifactory and pass them to the next stage
            this.removeNativeCopies(props1) // Q28: remove copies made by older versions
            props2 = this.currentPropertiesAsMap()
            if (this.file) {
                this.logger.debug '{}: repoKey: {}; pType: {}; hash: {}; current-props: {}', mName,
                    this.repoKey, this.pType, this.sha256, props2
            }
        } catch (e) {
            printTrace(mName, e)
            this.logger.error '{}: RlCommunityProcessor Failed: {}', context, e.toString(), e
            return [:]
        }
        return props2
    }

    /** Q28 (2.3.4): delete the secure.software.<native key> copies that versions before 2.3.4 wrote. */
    private void removeNativeCopies(Map props1) {
        String mName = 'removeNativeCopies'
        String context = "${this.what}::${mName}"
        try {
            for (String key in NativeCopies.keysToRemove(props1, this.namespace)) {
                this.logger.debug '{}: remove copied native property {}', context, key
                this.repositories.deleteProperty(this.repoPath, key)
            }
        } catch (e) {
            printTrace(mName, e)
            this.logger.error '{}: cannot remove copied native properties: {}', context, e.toString(), e
        }
    }

    private boolean doNewProps(Map newProps) {
        /* Apply the new props back to artifactory.
         * All new props have a explicit namespace set.
         * Old props have either no name space or
         *   the 'RL' namespace used by rl-scan-artifactory.
         */
        String mName = 'doNewProps'
        String context = "${this.what}::${mName}"
        this.logger.debug '{}: about to update props: {}', context, newProps
        this.logger.debug '{}: native keys (never written, all writes are namespaced): {}', context, this.originPropsClean

        try {
            for (item in newProps) {
                String key = "${item.key}"
                String value = "${item.value}"

                if (key == 'uuid') { continue } // uuid has no meaning after the api lookup

                if (key.startsWith('RL.')) { // they are not ours to change.
                    this.logger.debug '{}: skip RL. {}', context, key
                    continue
                }

                // Q26 (2.3.2): native properties (no namespace, no RL.) are safe because every key is written
                // with the namespace below. The old check compared the plain key ('status', 'purl', ...) with
                // the native keys, so a native 'status' stopped secure.software.status from being written.
                if (!key.startsWith(this.namespace)) {
                    this.logger.debug '{}: add namespace {} to key {}', context, this.namespace, key

                    key = "${this.namespace}.${item.key}"
                }

                this.logger.debug '{}: set prop {} -> {}', context, key, value
                this.repositories.setProperty(this.repoPath, key, value) // update/set.
            }
            return true
        } catch (e) {
            printTrace(mName, e)
            this.logger.error '{}: setProps Exception: {}', context, e.toString(), e
        }
        return false
    }

    private Map currentPropertiesAsMap() {
        String mName = 'currentPropertiesAsMap'
        String context = "${this.what}::${mName}"
        if (this.verbose) this.logger.info '{}:' , context // groovylint-disable DuplicateStringLiteral

        this.props = [:]
        try {
            this.myProperties = this.repositories.getProperties(this.repoPath)
            this.propertyKeys = this.myProperties.keys()
            for (String key in this.propertyKeys) {
                this.props[key] = this.myProperties.getFirst(key) ?: ''
            }
        } catch (e) {
            printTrace(mName, e)
            this.logger.error '{}: cannot fetch properties: {}', context, e.toString(), e
        }
        return this.props
    }

    private Map doBlockAnalizer(Map props2) {
        /* with the props given
         * we call BlockAnalizer to formulate a proper message and a verdict
         * if we have no explicit willBlock set we do not block (we return a empty map).
         * we return the result that contains now willBlock: true and a message.
         */
        String mName = 'doBlockAnalizer'
        String context = "${this.what}::${mName}"
        if (this.verbose) this.logger.info '{}:' , context // groovylint-disable DuplicateStringLiteral

        Map result = [:]

        // once we have all properties we can analize if we block or not
        try {
            this.ba = new BlockAnalizer(
                this.logger,
                this.verbose,
                context + '::BlockAnalizer',
                this.myConfig,
                this.path,
                this.repoKey,
                props2,
                this.clientAddress,
                this.haveCommunity,
            )
            result = ba.run()
            if (result.willBlock) { // we may be forced to pass , with a reason why message.
                return result
            }
        } catch (e) {
            printTrace(mName, e)
            this.logger.error '{}: BlockAnalizer Failed: {}', context, e.toString(), e
        }
        return [:]
    }

}

/*
// global context for a plugin.

property
    name: 'log',
    type: 'org.slf4j.Logger',
    doc: 'Writes to Artifactory log; logger name is the name of the script file '

property
    name: 'repositories',
    type: 'org.artifactory.repo.Repositories',
    doc: 'Allows queries and operations on repositories and artifacts '

property
    name: 'security',
    type: 'org.artifactory.security.Security',
    doc: 'Provides information about current security context, (e.g. current user and her permissions) '

property
    name: 'searches',
    type: 'org.artifactory.search.Searches',
    doc: 'API for searching for artifacts and builds'

property
    name: 'builds',
    type: 'org.artifactory.build.Builds',
    doc: 'Allows CRUD operations on builds'

property
    name: 'ctx',
    type: 'org.artifactory.spring.InternalArtifactoryContext',
    doc: 'Internal Artifactory context'
*/

@Field final int HTTP_500 = 500
@Field final int HTTP_403 = 403

download {
   // download events not starting with alt have no return values.

  /**
   * Handle before local download events.
   *
   * Closure parameters:
   *
   * request (org.artifactory.request.Request)
   *    - a read-only parameter of the request.
   *
   * responseRepoPath (org.artifactory.repo.RepoPath)
   *    - a read-only parameter of the response RepoPath
   *    (containing the physical repository the resource was found in).
   */
    beforeDownload { Request request, RepoPath responseRepoPath ->
        // beforeDownload has no return values
        // note beforeDownload happens after altResponse
        String what = 'beforeDownload'
        String path = responseRepoPath.toPath() ?: ''
        String context = "download.${what} ${path}"
        String clientAddress = request.clientAddress ?: ''
        String myPropertiesPath = 'plugins/rlArtifactoryPlugin.properties'
        String homeDir = "${ctx.artifactoryHome.etcDir}"
        String userAgent = 'rlArtifactoryPlugin v2.1.0'
        int verbose = 0

        if (!responseRepoPath.file) { return }
        if (path.startsWith('jfrog-usage-logs/')) { return } // skip internal jfrog items

        String msg
        Map result
        try {
            asSystem {
                RlArtifactoryAdapter raa = new RlArtifactoryAdapter(
                    log,
                    verbose,
                    what,
                    responseRepoPath,
                    repositories,
                    clientAddress,
                    homeDir,
                    myPropertiesPath,
                    userAgent,
                )
                result = raa.rlBlockVerdictOnDownload()
            }
            if (result.size() == 0) { // groovylint-disable DuplicateNumberLiteral
                return   // no block: continue as normal
            }
            msg = result.msg
            log.warn '{}: blocking item: {}', context, msg
        } catch (e) {
            e.printStackTrace(System.out)
            log.error '{}: rlBlockVerdictOnDownload error: {}', context, e.toString(), e
            return // groovylint-disable ReturnNullFromCatchBlock
        // lets not block the download on internal errors
        }
        throw new CancelException(msg, HTTP_403)
    }

    /**
    * Context variables:
    *
    * status (int)
    * - a response status code.
    *   Defaults to the response original status.
    *
    * message (java.lang.String)
    * - a text message to return in the response body,
    *   replacing the response content,
    *   or as an error message.
    *   Defaults to null.
    *
    * inputStream (java.io.InputStream)
    * - a new stream that provides the response content.
    *   Defaults to null.
    *
    * size (long)
    * - the size of the new content
    *   (helpful for clients processing the response).
    *   Defaults to -1.
    *
    * headers (java.util.Map<String,String>)
    * - Map containing the extra headers
    *   to override or add if not exists to the response.
    *   Usage example:
    *   headers = ["ExtraHeader":"SpecialHeader"]
    *
    * Closure parameters:
    * - request (org.artifactory.request.Request) - a read-only parameter of the request.
    *
    * - responseRepoPath (org.artifactory.repo.RepoPath) - a read-only parameter of the response RepoPath
    *   (containing the physical repository the resource was found in).
    *
    *  (For succeeded resolutions).
     */
    altResponse { Request request, RepoPath responseRepoPath ->
        // altResponse comes before remoteDownload
        // remoteDownload happens in case we dont have the item yet in <remote-repo>-cache.

        String what = 'altResponse'
        String path = responseRepoPath.toPath() ?: ''
        String context = "download.${what} ${path}"
        String clientAddress = request.clientAddress ?: ''
        String myPropertiesPath = 'plugins/rlArtifactoryPlugin.properties' // groovylint-disable DuplicateStringLiteral
        String homeDir = "${ctx.artifactoryHome.etcDir}"
        String userAgent = 'rlArtifactoryPlugin v2.1.0'
        int verbose = 0

        if (!responseRepoPath.file) { return }
        if (path.startsWith('jfrog-usage-logs/')) { // groovylint-disable DuplicateStringLiteral
            return // skip internal jfrog items
        }

        Map result
        try {
            asSystem {
                RlArtifactoryAdapter raa = new RlArtifactoryAdapter(
                    log,
                    verbose,
                    what,
                    responseRepoPath,
                    repositories,
                    clientAddress,
                    homeDir,
                    myPropertiesPath,
                    userAgent,
                )
                result = raa.rlBlockVerdictOnDownload()
            }
            if (result.size() == 0) { // groovylint-disable DuplicateNumberLiteral
                return // no block: continue as normal
            }
            log.warn "${context} ${result} ${HTTP_403}"
            message = result.msg
            status = HTTP_403
        } catch (e) {
            e.printStackTrace(System.out)
            log.error "Exception: ${context} rlBlockVerdictOnDownload Failed: ${e}"
            return // groovylint-disable ReturnNullFromCatchBlock
        // lets not block the download on internal errors
        }
    }
}
