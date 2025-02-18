import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.awt.GraphicsEnvironment
import java.io.ByteArrayOutputStream
import java.util.*

plugins {
    application
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.collektive)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.taskTree)
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        dependencies {
            implementation(libs.bundles.alchemist)
            implementation(libs.bundles.collektive)
            implementation(kotlin("reflect"))
            implementation(libs.caffeine)
            implementation(libs.rrmxmx)
        }
    }
}

multiJvm {
    jvmVersionForCompilation.set(17)
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    if (!GraphicsEnvironment.isHeadless()) {
        implementation("it.unibo.alchemist:alchemist-swingui:${libs.versions.alchemist.get()}")
    }
}

// Heap size estimation for batches
val maxHeap: Long? by project
val heap: Long =
    maxHeap ?: if (System.getProperty("os.name").lowercase().contains("linux")) {
        ByteArrayOutputStream()
            .use { output ->
                exec {
                    executable = "bash"
                    args = listOf("-c", "cat /proc/meminfo | grep MemAvailable | grep -o '[0-9]*'")
                    standardOutput = output
                }
                output.toString().trim().toLong() / 1024
            }.also { println("Detected ${it}MB RAM available.") } * 9 / 10
    } else {
        // Guess 16GB RAM of which 2 used by the OS
        14 * 1024L
    }
val taskSizeFromProject: Int? by project
val taskSize = taskSizeFromProject ?: 512
val threadCount = maxOf(1, minOf(Runtime.getRuntime().availableProcessors(), heap.toInt() / taskSize))
val alchemistGroupBatch = "Run batch simulations"
val alchemistGroupGraphic = "Run graphic simulations with Alchemist"
val alchemistGroupOptimizer = "Run optimizer simulations with Alchemist"

/*
 * This task is used to run all experiments in sequence
 */
val runAllGraphic by tasks.register<DefaultTask>("runAllGraphic") {
    group = alchemistGroupGraphic
    description = "Launches all simulations with the graphic subsystem enabled"
}
val runAllBatch by tasks.register<DefaultTask>("runAllBatch") {
    group = alchemistGroupBatch
    description = "Launches all experiments in batch mode"
}
val runAllOptimizer by tasks.register<DefaultTask>("runAllOptimizer") {
    group = alchemistGroupOptimizer
    description = "Launches all experiments with the optimizer enabled"
}

fun String.capitalizeString(): String =
    this.replaceFirstChar {
        if (it.isLowerCase()) {
            it.titlecase(
                Locale.getDefault(),
            )
        } else {
            it.toString()
        }
    }

/*
 * Scan the folder with the simulation files, and create a task for each one of them.
 */
File(rootProject.rootDir.path + "/src/main/yaml")
    .listFiles()
    ?.filter { it.extension == "yml" }
    ?.sortedBy { it.nameWithoutExtension }
    ?.forEach {
        fun basetask(
            name: String,
            additionalConfiguration: JavaExec.() -> Unit = {},
        ) = tasks.register<JavaExec>(name) {
            description = "Launches graphic simulation ${it.nameWithoutExtension}"
            mainClass.set("it.unibo.alchemist.Alchemist")
            classpath = sourceSets["main"].runtimeClasspath
            args("run", it.absolutePath)
            if (System.getenv("CI") == "true") {
                args(
                    "--override",
                    "terminate: { type: AfterTime, parameters: [2] } ",
                )
            } else {
                this.additionalConfiguration()
            }
        }
        val capitalizedName = it.nameWithoutExtension.capitalizeString()
        val graphic by basetask("run${capitalizedName}Graphic") {
            group = alchemistGroupGraphic
            args(
                "--override",
                "monitors: { type: SwingGUI, parameters: { graphics: effects/${it.nameWithoutExtension}.json } }",
                "--override",
                "launcher: { parameters: { batch: [], autoStart: false } }",
                "--verbosity",
                "error",
            )
        }
        runAllGraphic.dependsOn(graphic)
        val batch by basetask("run${capitalizedName}Batch") {
            group = alchemistGroupBatch
            description = "Launches batch experiments for $capitalizedName"
            maxHeapSize = "${minOf(heap.toInt(), Runtime.getRuntime().availableProcessors() * taskSize)}m"
            File("data").mkdirs()
            if (capitalizedName == "FixedLeader") {
                args(
                    "--override",
                    """
                    launcher:
                      type: DefaultLauncher
                      parameters: {
                        batch: ["seed", "maxResource", "maxSuccess", "resourceLowerBound"],
                        autoStart: true
                      }
                    """.trimIndent(),
                )
            } else {
                args(
                    "--override",
                    """
                    launcher:
                      type: DefaultLauncher
                      parameters: {
                        batch: ["seed"],
                        autoStart: true,
                      }
                    """.trimIndent(),
                    "--verbosity",
                    "error",
                )
            }
        }
        runAllBatch.dependsOn(batch)
        val optimizer by basetask("run${capitalizedName}Optimizer") {
            group = alchemistGroupOptimizer
            description = "Launches Nelder Mead parameters optimizer for $capitalizedName"
            maxHeapSize = "${minOf(heap.toInt(), Runtime.getRuntime().availableProcessors() * taskSize)}m"
            File("data").mkdirs()
            args(
                "--override",
                """
                variables:
                  goal: &goal
                    formula: |
                      it.unibo.Goal()
                    language: kotlin
                    
                launcher:
                  type: it.unibo.alchemist.boundary.launchers.NelderMeadLauncher
                  parameters: {
                    objectiveFunction: *goal,
                    variables: ["maxResource", "resourceLowerBound", "maxSuccess"],
                    seedName: "seed",
                    repetitions: 15,
                    maxIterations: 15,
                  }
                """.trimIndent(),
            )
        }
        runAllOptimizer.dependsOn(optimizer)
    }

tasks.withType(KotlinCompile::class).all {
    compilerOptions {
        allWarningsAsErrors = false
    }
}
