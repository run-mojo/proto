package run.mojo.wire.compiler

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/**
 * Generates WireMessage adapters and schemas from Pojos decorated with standard annotations.
 *
 * @author Clay Molocznik
 */
@SupportedAnnotationTypes("*")
class GeneratorProcessor : AbstractProcessor() {

    private var processor: WireProcessor? = null
    private var config: Config? = null

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        config = Config.create(processingEnv)
        processor = WireProcessor.create(processingEnv, config!!)
    }

    /** @return
     */
    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedOptions(): Set<String> {
        return Config.SUPPORT_OPTIONS
    }

    /** @return
     */
    override fun getSupportedAnnotationTypes(): Set<String> {
        return processor!!.config.annotations
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        return processor!!.process(annotations, roundEnv)
    }

}
