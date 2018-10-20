package run.mojo.wire.compiler

import run.mojo.wire.Wire
import run.mojo.wire.annotations.ImportModels
import run.mojo.wire.annotations.ImportAction

import javax.annotation.processing.ProcessingEnvironment
import java.util.*
import java.util.stream.Collectors

class Config(
    val actionLinkers: List<ActionLinker>,
    val optionActionAnnotation: String,
    val optionActionResolve: String,
    val optionActionDispatcher: String,
    val annotations: Set<String>,
    val lombokData: Boolean = true,
    val lombokValue: Boolean = true
) {


    companion object {
        val OPTION_ACTION_ANNOTATION = "wire_action_annotation"
        val OPTION_ACTION_RESOLVE = "wire_action_resolve"
        val OPTION_ACTION_DISPATCHER = "wire_action_dispatcher"
        val OPTION_LOMBOK_DATA = "wire_lombok_data"
        val OPTION_LOMBOK_VALUE = "wire_lombok_value"

        val SUPPORT_OPTIONS = setOf(
            OPTION_ACTION_ANNOTATION,
            OPTION_ACTION_RESOLVE,
            OPTION_ACTION_DISPATCHER,
            OPTION_LOMBOK_DATA,
            OPTION_LOMBOK_VALUE
        )

        internal val LOMBOK_DATA = "lombok.Data"
        internal val LOMBOK_VALUE = "lombok.Value"
        internal val LOMBOK_BUILDER = "lombok.MessageBuilder"

        fun create(processingEnv: ProcessingEnvironment): Config {
            val optionActionAnnotation = processingEnv.options[Config.OPTION_ACTION_ANNOTATION]
            val actionLinkers: List<ActionLinker>

            if (optionActionAnnotation != null) {
                actionLinkers =
                        Arrays.stream(optionActionAnnotation
                            .toString().split(";").dropLastWhile { it.isEmpty() }.toTypedArray()
                        )
                            .map<ActionLinker> { ActionLinker.create(it) }
                            .filter { Objects.nonNull(it) }
                            .collect(Collectors.toList())
            } else {
                actionLinkers = emptyList()
            }
            val optionActionResolve = processingEnv.options[OPTION_ACTION_RESOLVE]
            val optionActionDispatcher = processingEnv.options[OPTION_ACTION_DISPATCHER]

            val lombokData = processingEnv.options[OPTION_LOMBOK_DATA]
            val lombokValue = processingEnv.options[OPTION_LOMBOK_VALUE]

            val annotations = LinkedHashSet<String>()
            annotations.add(LOMBOK_DATA)
            annotations.add(LOMBOK_VALUE)
            annotations.add(LOMBOK_BUILDER)
            annotations.add(Wire::class.java.canonicalName)
            annotations.add(ImportAction::class.java.canonicalName)
            annotations.add(ImportModels::class.java.canonicalName)

            return Config(
                actionLinkers,
                optionActionAnnotation ?: "",
                optionActionResolve ?: "",
                optionActionDispatcher ?: "",
                annotations,
                if (lombokData == "N") false else true,
                if (lombokValue == "N") false else true
            )
        }
    }
}

data class MessageCondition(val annotation: String)

data class ModelAnnotation(val name: String)

/**
 * Provides for custom user defined annotations to link an Action and it's model graph.
 *
 * @MyCustomAnnotation()
 */
data class ActionLinker(
    val name: String,
    val annotatedWith: String,
    val baseType: String,
    val requestIndex: Int,
    val responseIndex: Int) {

    companion object {

        fun create(text: String): ActionLinker? {
            val parts = text.split("[,]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size != 5) {
                return null
            }
            val name = parts[0].trim { it <= ' ' }
            val annotationName = parts[1].trim { it <= ' ' }
            val type = parts[2].trim { it <= ' ' }
            var request = -1
            var response = -1
            try {
                request = Integer.parseInt(parts[3].trim { it <= ' ' })
            } catch (e: Throwable) {
                // Ignore.
            }

            try {
                response = Integer.parseInt(parts[4].trim { it <= ' ' })
            } catch (e: Throwable) {
                // Ignore.
            }

            return ActionLinker(name, annotationName, type, request, response)
        }
    }
}
