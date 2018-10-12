package com.zhangls.android.verification

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatEditText
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import kotlin.math.min


/**
 * Created by zhangls on 2018/8/31.
 *
 * 自定义验证码输入框组件。支持下划线，方框等形式
 */
class VerificationCodeInputBox : AppCompatEditText {

    /**
     * 输入框类型，line：横线、square：方框
     */
    private var mBoxStyle = STYLE_LINE
    /**
     * 输入框正常时的颜色
     */
    private var mNormalBgColor = ContextCompat.getColor(context, R.color.colorAccent)
    /**
     * 输入框选中时的颜色
     */
    private var mPressBgColor = ContextCompat.getColor(context, R.color.colorPrimary)
    /**
     * 验证码数字颜色
     */
    private var mTextColor = ContextCompat.getColor(context, R.color.colorAccent)
    /**
     * 验证码数字大小
     */
    private var mTextSize = resources.getDimensionPixelSize(R.dimen.defBoxTextSize)
    /**
     * 输入框线条宽度
     */
    private var mLineWidth = resources.getDimensionPixelSize(R.dimen.defBoxLineWidth)
    /**
     * 输入框圆角半径
     */
    private var mRoundRadius = 0
    /**
     * 输入框之间的间隔
     */
    private var mItemWidth = resources.getDimensionPixelSize(R.dimen.defBoxItemWidth)
    /**
     * 输入框之间的间隔
     */
    private var mSpaceWidth = resources.getDimensionPixelSize(R.dimen.defBoxSpaceWidth)
    /**
     * 输入框数量
     */
    private var mBoxCount = BOX_COUNT
    /**
     * 输入完成后回调接口
     */
    var onInputComplete: (String) -> Unit = { _ -> }
    /**
     * 原始宽度（不受影响时的宽度）
     */
    private var mRawWidth = 0
    /**
     * 原始高度（不受影响时的高度）
     */
    private var mRawHeight = 0
    /**
     * 当前输入的文本
     */
    private var mCurrentInputText = ""
    /**
     * 默认状态的输入框画笔
     */
    private lateinit var mNormalPaint: Paint
    /**
     * 选中状态的输入框画笔
     */
    private lateinit var mPressPaint: Paint
    /**
     * 数字画笔
     */
    private lateinit var mTextPaint: Paint
    /**
     * 用于绘制圆角矩形
     */
    private var mRectF = RectF()


    companion object {
        // 输入框类型，line：横线、square：方框
        private const val STYLE_LINE = 0
        private const val STYLE_SQUARE = 1

        // 输入框数量
        private const val BOX_COUNT = 6
    }

    constructor(context: Context) : super(context) {
        initBoxView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initBoxView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initBoxView(context, attrs, defStyleAttr)
    }

    /**
     * 初始化组建配置，读取属性值
     */
    private fun initBoxView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) {
        // 读取属性值
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerificationCodeInputBox, defStyleAttr, 0)

        mBoxStyle = typedArray.getInt(R.styleable.VerificationCodeInputBox_boxStyle, mBoxStyle)
        mBoxCount = typedArray.getInt(R.styleable.VerificationCodeInputBox_boxCount, mBoxCount)

        mNormalBgColor = typedArray.getColor(R.styleable.VerificationCodeInputBox_boxNormalColor, mNormalBgColor)
        mPressBgColor = typedArray.getColor(R.styleable.VerificationCodeInputBox_boxPressColor, mPressBgColor)
        mTextColor = typedArray.getColor(R.styleable.VerificationCodeInputBox_boxTextColor, mTextColor)

        mTextSize = typedArray.getDimensionPixelSize(R.styleable.VerificationCodeInputBox_boxTextSize, mTextSize)
        mLineWidth = typedArray.getDimensionPixelSize(R.styleable.VerificationCodeInputBox_boxLineWidth, mLineWidth)
        mRoundRadius = typedArray.getDimensionPixelSize(R.styleable.VerificationCodeInputBox_boxRoundRadius, mRoundRadius)
        mItemWidth = typedArray.getDimensionPixelSize(R.styleable.VerificationCodeInputBox_boxItemWidth, mItemWidth)
        mSpaceWidth = typedArray.getDimensionPixelSize(R.styleable.VerificationCodeInputBox_boxSpaceWidth, mSpaceWidth)

        typedArray.recycle()

        initEditText()
        calculateRawSize()
        initPaints()
    }

    /**
     * 初始化文本框配置
     */
    private fun initEditText() {
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        setTextColor(Color.TRANSPARENT)

        inputType = InputType.TYPE_CLASS_NUMBER

        isCursorVisible = false

        // 确保点击后，光标能在最后
        setOnClickListener { setSelection(text!!.length) }
        isLongClickable = false
        setTextIsSelectable(false)
        // 设置最大字符数
        filters = arrayOf(InputFilter.LengthFilter(mBoxCount))
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputText = s.toString()
                if (inputText.length <= mBoxCount) {
                    mCurrentInputText = inputText
                    invalidate()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().length == mBoxCount) onInputComplete(s.toString())
            }
        })

        // 防止阻止键盘收起
        setOnEditorActionListener { _, _, _ -> false }
    }

    /**
     * 计算内容绘制区域的原始尺寸
     */
    private fun calculateRawSize() {
        mRawWidth = mItemWidth * mBoxCount + mSpaceWidth * (mBoxCount - 1)
        mRawHeight = mItemWidth
    }

    /**
     * 初始化画笔
     */
    private fun initPaints() {
        mNormalPaint = with(Paint()) {
            style = Paint.Style.STROKE
            isDither = true
            isAntiAlias = true
            color = mNormalBgColor
            strokeWidth = mLineWidth.toFloat()
            this
        }

        mPressPaint = with(Paint()) {
            style = Paint.Style.STROKE
            isDither = true
            isAntiAlias = true
            color = mPressBgColor
            strokeWidth = mLineWidth.toFloat()
            this
        }

        mTextPaint = with(Paint()) {
            style = Paint.Style.STROKE
            isDither = true
            isAntiAlias = true
            color = mTextColor
            textAlign = Paint.Align.CENTER
            textSize = mTextSize.toFloat()
            this
        }
    }

    /**
     * 根据设置的宽高，确定组件大小
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        var widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        // 如果设置的宽高为 wrap_content,则可以完全绘制出符合要求的输入框，所以可以输出输入框的原始宽高
        when {
            widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST -> {
                widthSize = mRawWidth + paddingLeft + paddingRight
                heightSize = mRawHeight + paddingTop + paddingBottom
            }
            widthMode == MeasureSpec.AT_MOST -> widthSize = mRawWidth + paddingLeft + paddingRight
            heightMode == MeasureSpec.AT_MOST -> heightSize = mRawHeight + paddingTop + paddingBottom
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    /**
     * 绘制输入框、数字
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // 确定四个边距的位置
        val realLeft = paddingLeft
        val realRight = width - paddingRight
        val realTop = paddingTop
        val realBottom = height - paddingBottom

        // 计算每个 item 的尺寸以及间隙距，确保正确显示
        // 宽高都会影响 item 的大小

        // 比较实际可绘制高度与原始高度，取最小的值
        val itemSize = min(realBottom - realTop, mRawHeight)

        // 计算出实际的间隔，取计算出的间隔与设定的值中的最小值
        var drawItemSpaceSize = (realRight - realLeft - itemSize * mBoxCount) / (mBoxCount - 1)
        drawItemSpaceSize = min(drawItemSpaceSize, mSpaceWidth)

        // 实际的内容宽度
        val drawRealWidth = (drawItemSpaceSize + itemSize) * mBoxCount - drawItemSpaceSize

        // 根据显示的位置来确定左边距
        val gapWidth = realRight - realLeft - drawRealWidth
        val leftWidth = gapWidth / 2

        // 确定文字的 baseline
        val fontBottom = mTextPaint.fontMetrics?.bottom ?: 0F
        val fontTop = mTextPaint.fontMetrics?.top ?: 0F
        val fontY = (height - fontBottom + fontTop) / 2 - fontTop

        // 确定各个图的位置、文字位置，并画图
        val currentInputTextLen = mCurrentInputText.length

        for (index in 0 until mBoxCount) {
            val left = (leftWidth + realLeft + (itemSize + drawItemSpaceSize) * index).toFloat()
            val right = left + itemSize
            val bottom = (realTop + itemSize).toFloat()

            val mPaint = if (index < currentInputTextLen) mPressPaint else mNormalPaint

            when (mBoxStyle) {
                STYLE_LINE -> canvas?.drawLine(left, bottom, right, bottom, mPaint)
                STYLE_SQUARE -> {
                    mRectF.left = left
                    mRectF.top = realTop.toFloat()
                    mRectF.right = right
                    mRectF.bottom = bottom

                    canvas?.drawRoundRect(mRectF, mRoundRadius.toFloat(), mRoundRadius.toFloat(), mPaint)
                }
            }

            canvas?.drawText(
                    if (index < currentInputTextLen) mCurrentInputText[index].toString() else "",
                    left + (right - left) / 2F,
                    fontY,
                    mTextPaint)
        }
    }
}