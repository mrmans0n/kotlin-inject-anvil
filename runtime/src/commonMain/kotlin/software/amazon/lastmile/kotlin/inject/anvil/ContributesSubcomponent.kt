package software.amazon.lastmile.kotlin.inject.anvil

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Generates a subcomponent when the parent component interface is merged.
 *
 * Imagine this module dependency tree:
 * ```
 *        :app
 *       /     \
 *      v       v
 *   :lib-a   :lib-b
 * ```
 * `:app` creates the component with `@MergeComponent`, `:lib-a` creates a subcomponent with its
 * own `@Component` and `@MergeComponent` and `:lib-b` contributes a component to the scope of the
 * subcomponent. This component won't be included in the subcomponent without adding the dependency
 * `:lib-a -> :lib-b`.
 *
 * On the other hand, if `:lib-a` uses `@ContributesSubcomponent` with a parent scope of the main
 * component from `:app`, then the actual subcomponent will be generated in the `:app` module and
 * the contributed module from `:lib-b` will be picked up.
 *
 * ```
 * @ContributesSubcomponent(LoggedInScope::class)
 * @SingleIn(LoggedInScope::class)
 * interface LoggedInComponent {
 *
 *     @ContributesSubcomponent.Factory(AppScope::class)
 *     interface Factory {
 *         fun createLoggedInComponent(): LoggedInComponent
 *     }
 * }
 * ```
 * This is the typical setup. The `Factory` interface will be implemented by the parent scope
 * `AppScope::class` and the final `LoggedInComponent` will be generated when `@MergeComponent`
 * is used, e.g. this would trigger generating the subcomponent.
 * ```
 * @Component
 * @MergeComponent(AppScope::class)
 * @SingleIn(AppScope::class)
 * abstract class AppComponent : AppComponentMerged
 * ```
 * The generated code for the subcomponent would look like:
 * ```
 * // Implements the body of the factory function.
 * interface AppComponentMerged : LoggedInComponent.Factory {
 *     override fun createLoggedInComponent(): LoggedInComponent {
 *         return LoggedInComponentFinal.create(this)
 *     }
 * }
 *
 * @Component
 * @MergeComponent(LoggedInScope::class)
 * @SingleIn(LoggedInScope::class)
 * abstract class LoggedInComponentFinal(
 *     @Component val parentComponent: AppComponent
 * ) : LoggedInComponent, LoggedInComponentMerged
 * ```
 *
 * A chain of [ContributesSubcomponent]s is supported.
 *
 * ## Component arguments
 *
 * Parameters on the factory function are forwarded to the generated component and bound in the
 * component, e.g.
 * ```
 * @ContributesSubcomponent.Factory(AppScope::class)
 * interface Factory {
 *     fun createComponent(string: String, int: Int): ChildComponent
 * }
 * ```
 * would instantiate the final component like this:
 * ```
 * abstract class ChildComponentFinal(
 *     @Component val parentComponent: ParentComponent,
 *     @get:Provides val string: String,
 *     @get:Provides val int: Int,
 * ): ChildComponent, ChildComponentFinalMerged
 *
 * override fun createComponent(string: String, int: Int): ChildComponent {
 *     return ChildComponentFinal.create(parentComponent, string, int)
 * }
 * ```
 * Contributing abstract classes is not supported, e.g. the following is not supported and the
 * parameter can be moved to the factory instead and then the abstract class can be converted to
 * an interface.
 * ```
 * // Remove the parameter and convert the class to an interface. The parameter will be
 * // generated due to the Factory having this parameter.
 * @ContributesSubcomponent(LoggedInScope::class)
 * @SingleIn(LoggedInScope::class)
 * abstract class LoggedInComponent(
 *     @get:Provides val string: String,
 * ) {
 *
 *     @ContributesSubcomponent.Factory(AppScope::class)
 *     interface Factory {
 *         fun createLoggedInComponent(string: String): LoggedInComponent
 *     }
 * }
 * ```
 */
@Target(CLASS)
public annotation class ContributesSubcomponent(
    /**
     * The scope in which to include this contributed component interface.
     */
    val scope: KClass<*>,
) {
    /**
     * A factory for the contributed subcomponent.
     *
     * Each contributed subcomponent must have a factory interface as inner class. The body of the
     * factory function will be generated when the parent component is merged.
     *
     * The factory interface must have a single function with the contributed subcomponent as
     * return type. Parameters are supported as mentioned in [ContributesSubcomponent].
     */
    public annotation class Factory(
        /**
         * The scope in which to include this contributed component interface.
         */
        val scope: KClass<*>,
    )
}
