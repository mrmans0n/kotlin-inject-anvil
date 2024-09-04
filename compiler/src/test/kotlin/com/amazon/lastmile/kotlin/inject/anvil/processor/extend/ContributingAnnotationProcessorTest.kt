@file:OptIn(ExperimentalCompilerApi::class)

package com.amazon.lastmile.kotlin.inject.anvil.processor.extend

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.amazon.lastmile.kotlin.inject.anvil.compile
import com.amazon.lastmile.kotlin.inject.anvil.contributesRenderer
import com.amazon.lastmile.kotlin.inject.anvil.internal.Origin
import com.amazon.lastmile.kotlin.inject.anvil.propertyAnnotations
import com.amazon.lastmile.kotlin.inject.anvil.propertyMethodGetter
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ContributingAnnotationProcessorTest {

    @Test
    fun `a property is generated in the lookup package for a contributing annotation`() {
        compile(
            """
            package com.amazon.test
    
            import com.amazon.lastmile.kotlin.inject.anvil.extend.ContributingAnnotation
            import kotlin.annotation.AnnotationTarget.CLASS

            @ContributingAnnotation
            @Target(CLASS)
            annotation class ContributesRenderer
            """,
        ) {
            val propertyGetter = contributesRenderer.propertyMethodGetter

            // The type parameter gets erased at runtime.
            assertThat(propertyGetter.returnType.kotlin).isEqualTo(KClass::class)

            // Checks the returned type.
            assertThat(propertyGetter.invoke(null)).isEqualTo(contributesRenderer.kotlin)

            // The @Origin annotation is present.
            assertThat(
                contributesRenderer.propertyAnnotations.filterIsInstance<Origin>().single().value,
            ).isEqualTo(contributesRenderer.kotlin)
        }
    }

    @Test
    fun `a contributing annotation interface must be public`() {
        compile(
            """
            package com.amazon.test
    
            import com.amazon.lastmile.kotlin.inject.anvil.extend.ContributingAnnotation
            import kotlin.annotation.AnnotationTarget.CLASS

            @ContributingAnnotation
            @Target(CLASS)
            internal annotation class ContributesRenderer
            """,
            exitCode = COMPILATION_ERROR,
        ) {
            assertThat(messages).contains("Contributing annotations must be public.")
        }
    }
}
