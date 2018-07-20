package icfpc2018.util

import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import gln.glClearColor
import gln.glViewport
import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import uno.glfw.GlfwWindow
import uno.glfw.glfw

fun main(args: Array<String>) {

    with(Window()) {
        run()
        end()
    }
}

val windowSize = Vec2i(1200, 800)

abstract class AbstractGlfwWindow {
    private val window = initWindow("Test")

    abstract fun scene()

    fun run() {
        while (window.open) {
            window.processInput()

            scene()

            window.swapBuffers()
            glfw.pollEvents()
        }
    }

    fun end() {
        window.destroy()
        glfw.terminate()
    }

    private fun GlfwWindow.processInput() {
        if (pressed(GLFW_KEY_ESCAPE)) close = true
    }

    private fun initWindow(title: String): GlfwWindow {
        with(glfw) {
            init()
            windowHint {
                context.version = "3.3"
                profile = "core"
            }
        }
        return GlfwWindow(windowSize, title).apply {
            makeContextCurrent()
            show()
            framebufferSizeCallback = { size -> glViewport(size) }
        }.also {
            GL.createCapabilities()
        }
    }
}

class Window: AbstractGlfwWindow() {
    private val backgroundColor = Vec4(0.2f, 0.3f, 0.3f, 1f)

    override fun scene() {
        glClearColor(backgroundColor)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

    }
}




