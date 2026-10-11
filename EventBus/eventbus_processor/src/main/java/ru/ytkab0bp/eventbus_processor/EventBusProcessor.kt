package ru.ytkab0bp.eventbus_processor

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import ru.ytkab0bp.eventbus.Event
import ru.ytkab0bp.eventbus.EventBusCompatConsumer
import ru.ytkab0bp.eventbus.EventBusListenerImpl
import ru.ytkab0bp.eventbus.EventHandler
import java.io.IOException
import java.util.HashMap
import java.util.HashSet
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.FilerException
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class EventBusProcessor : AbstractProcessor() {
    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment,
    ): Boolean {
        if (roundEnv.rootElements.isEmpty()) return true

        val packageName = roundEnv.rootElements
            .firstOrNull { it.toString().endsWith(".BuildConfig") }
            ?.toString()
            ?.substringBeforeLast('.')
            ?: return true

        val staticBlock = CodeBlock.builder()
        val canFireParent = MethodSpec.methodBuilder("canFireParent")
            .returns(Boolean::class.javaPrimitiveType!!)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Class::class.java, "clz")
            .addAnnotation(Override::class.java)
            .addCode("return $PARENT_MAP.contains(clz);")

        for (element in roundEnv.getElementsAnnotatedWith(Event::class.java)) {
            val event = element.getAnnotation(Event::class.java) ?: continue
            if (event.canFireParent) {
                staticBlock.add("$PARENT_MAP.add($element.class);\n")
            }
        }

        val implementation = TypeSpec.classBuilder(packageName.replace(".", "_Z9_"))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(EventBusListenerImpl::class.java)
            .addMethod(canFireParent.build())
            .addField(
                FieldSpec.builder(
                    ParameterizedTypeName.get(
                        Map::class.java,
                        Class::class.java,
                        EventBusCompatConsumer::class.java,
                    ),
                    IMPL_MAP,
                    Modifier.PRIVATE,
                    Modifier.FINAL,
                    Modifier.STATIC,
                ).initializer("new java.util.HashMap<>()").build(),
            )
            .addField(
                FieldSpec.builder(
                    ParameterizedTypeName.get(List::class.java, Class::class.java),
                    PARENT_MAP,
                    Modifier.PRIVATE,
                    Modifier.FINAL,
                    Modifier.STATIC,
                ).initializer("new java.util.ArrayList<>()").build(),
            )

        val onEvent = MethodSpec.methodBuilder("onEvent")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Any::class.java, "obj")
            .addParameter(Any::class.java, "event")
            .addAnnotation(Override::class.java)

        val classData = HashMap<String, CodeBlock.Builder>()
        for (method in roundEnv.getElementsAnnotatedWith(EventHandler::class.java)) {
            val listenerClass = method.enclosingElement
            val listenerClassName = listenerClass.toString()
            val methodCode = classData.getOrPut(listenerClassName) { CodeBlock.builder() }
            val handler = method.getAnnotation(EventHandler::class.java) ?: continue
            val signature = method.toString()
            val eventType = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'))

            val reason = when {
                Modifier.PUBLIC !in method.modifiers || Modifier.STATIC in method.modifiers ->
                    CannotProcessReason.METHOD_ACCESS
                Modifier.PUBLIC !in listenerClass.modifiers -> CannotProcessReason.CLASS_ACCESS
                eventType.isEmpty() -> CannotProcessReason.NOT_ENOUGH_ARGUMENTS
                eventType.count { it == ',' } > 0 -> CannotProcessReason.TOO_MANY_ARGUMENTS
                else -> null
            }

            if (reason != null) {
                System.err.println(
                    "[!] EventBus can not process this method: $listenerClassName@$method, reason: ${reason.reason}",
                )
                continue
            }

            val methodName = signature.substringBeforeLast('(')
            methodCode.add("if (ru.ytkab0bp.eventbus.EventBus.canFire($eventType.class, event)) {\n")
            if (handler.runOnMainThread) {
                methodCode.add("ru.ytkab0bp.eventbus.EventBus.postOnUiThread(()->{\n")
            }
            methodCode.add("$listenerClassName cObj = ($listenerClassName) obj;\n")
            methodCode.add("cObj.$methodName(($eventType) event);\n")
            if (handler.runOnMainThread) methodCode.add("});\n")
            methodCode.add("}\n")
        }

        for ((className, code) in classData) {
            staticBlock.add("$IMPL_MAP.put($className.class, (obj,event) -> {\n")
            staticBlock.add(code.build())
            staticBlock.add("});\n")
        }

        onEvent.addCode("for (java.util.Map.Entry<Class, EventBusCompatConsumer> en : $IMPL_MAP.entrySet()) {\n")
        onEvent.addCode("if (en.getKey().isInstance(obj)) en.getValue().onEvent(obj, event);\n")
        onEvent.addCode("}\n")
        implementation.addMethod(onEvent.build())
        implementation.addStaticBlock(staticBlock.build())

        try {
            JavaFile.builder("ru.ytkab0bp.eventbus.impl", implementation.build())
                .build()
                .writeTo(processingEnv.filer)
        } catch (_: FilerException) {
            // Another annotation-processing round already emitted the package implementation.
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
        return true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> = HashSet(
        listOf(Event::class.java.canonicalName, EventHandler::class.java.canonicalName),
    )

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.RELEASE_8

    private enum class CannotProcessReason(val reason: String) {
        NOT_ENOUGH_ARGUMENTS("Not enough arguments!"),
        TOO_MANY_ARGUMENTS("Too many arguments!"),
        CLASS_ACCESS("Class access is not public!"),
        METHOD_ACCESS("Method access is not public!"),
    }

    companion object {
        private const val IMPL_MAP = "objMap"
        private const val PARENT_MAP = "parentMap"
    }
}
