package ru.netology.statsview.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.withStyledAttributes
import ru.netology.statsview.R
import ru.netology.statsview.utils.AndroidUtils
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class StatsView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(
    context,
    attributeSet,
    defStyleAttr,
    defStyleRes
) {
    private var textSize = AndroidUtils.dp(context, 20).toFloat()
    private var lineWidth = AndroidUtils.dp(context, 5)
    private var colors = emptyList<Int>()

    // Анимационные параметры
    private var rotationProgress = 0f // от 0 до 1
    private var fillProgress = 0f // от 0 до 1
    private var rotationAnimator: ValueAnimator? = null
    private var fillAnimator: ValueAnimator? = null

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StatsView) {
            textSize = getDimension(R.styleable.StatsView_textSize, textSize)
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth.toFloat()).toInt()

            colors = listOf(
                getColor(R.styleable.StatsView_colors1, generateRandomColor()),
                getColor(R.styleable.StatsView_colors2, generateRandomColor()),
                getColor(R.styleable.StatsView_colors3, generateRandomColor()),
                getColor(R.styleable.StatsView_colors4, generateRandomColor()),
            )
        }
    }

    var data: List<Float> = emptyList()
        set(value) {
            field = value
            calculateProportions()
            // Сбрасываем анимацию при новых данных
            rotationProgress = 0f
            fillProgress = 0f
            startAnimation()
            invalidate()
        }

    private var proportions: List<Float> = emptyList()

    private var radius = 0F
    private var center = PointF()
    private var oval = RectF()
    private val paint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        strokeWidth = lineWidth.toFloat()
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        textSize = this@StatsView.textSize
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private fun calculateProportions() {
        if (data.isEmpty()) {
            proportions = emptyList()
            return
        }

        val sum = data.sum()

        if (sum == 0f) {
            val equalShare = 1f / data.size
            proportions = List(data.size) { equalShare }
        } else {
            proportions = data.map { it / sum  }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2F - lineWidth
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) {
            return
        }

        // Рисуем линии с поворотом
        var startAngle = -90F + (rotationProgress * 360f ) // Добавляем поворот к начальному углу
        val firstColor = colors.getOrElse(0) { generateRandomColor() }

        proportions.forEachIndexed { index, proportion ->
            // Применяем прогресс заполнения к углу сегмента
            val fullAngle = proportion * 360F
            val animatedAngle = fullAngle * fillProgress

            paint.color = colors.getOrElse(index) { generateRandomColor() }
            canvas.drawArc(oval, startAngle, animatedAngle, false, paint)
            startAngle += fullAngle // Используем полный угол для позиционирования
        }

        // Рисуем точку только если есть прогресс анимации
        if (fillProgress > 0) {
            val pointAngle = -90F + (rotationProgress * 360f ) // Точка тоже вращается
            val startRad = Math.toRadians(pointAngle.toDouble())
            val startX = center.x + radius * cos(startRad).toFloat()
            val startY = center.y + radius * sin(startRad).toFloat()

            val originalStyle = paint.style
            paint.style = Paint.Style.FILL
            paint.color = firstColor
            canvas.drawCircle(startX, startY, lineWidth / 2f, paint)
            paint.style = originalStyle
        }

        // Текст рисуется всегда без поворота
        canvas.drawText(
            "%.2f%%".format(proportions.sum() * 100),
            center.x,
            center.y + textPaint.textSize / 4,
            textPaint
        )
    }

    fun onTextDraw(canvas: Canvas) {
        canvas.drawText(
            "%.2f%%".format(proportions.sum() * 100),
            center.x,
            center.y + textPaint.textSize / 4,
            textPaint
        )
    }

    private fun startAnimation() {
        // Останавливаем предыдущие анимации
        rotationAnimator?.cancel()
        fillAnimator?.cancel()

        // Анимация поворота - 1 полный оборот за 2 секунды
        rotationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000L
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                rotationProgress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Анимация заполнения - постепенное появление линий
        fillAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500L
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                fillProgress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // Метод для остановки анимации
    fun stopAnimation() {
        rotationAnimator?.cancel()
        fillAnimator?.cancel()
        rotationProgress = 0f
        fillProgress = 1f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rotationAnimator?.cancel()
        fillAnimator?.cancel()
    }

    private fun generateRandomColor() = Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
}