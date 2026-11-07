package com.example.data

import com.example.data.dao.WhiteboardDao
import com.example.data.entity.WhiteboardEntity
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class WhiteboardRepository(private val dao: WhiteboardDao) {

    val allBoards: Flow<List<WhiteboardEntity>> = dao.getAllWhiteboards()

    fun getBoard(id: Long): Flow<WhiteboardEntity?> = dao.getWhiteboardById(id)

    suspend fun saveBoard(
        id: Long,
        title: String,
        subject: String,
        backgroundType: BackgroundType,
        theme: BoardTheme,
        strokes: List<Stroke>,
        shapes: List<ShapeObject>,
        textBlocks: List<TextBlockObject>,
        graphs: List<GraphObject>,
        pageNumber: Int = 1
    ): Long {
        val strokesJson = serializeStrokes(strokes)
        val shapesJson = serializeShapes(shapes)
        val textBlocksJson = serializeTextBlocks(textBlocks)
        val graphsJson = serializeGraphs(graphs)

        val entity = WhiteboardEntity(
            id = if (id > 0) id else 0,
            title = title,
            subject = subject,
            backgroundType = backgroundType.name,
            themeName = theme.name,
            strokesJson = strokesJson,
            shapesJson = shapesJson,
            textBlocksJson = textBlocksJson,
            graphsJson = graphsJson,
            pageNumber = pageNumber,
            updatedAt = System.currentTimeMillis()
        )
        return dao.insertWhiteboard(entity)
    }

    suspend fun deleteBoard(id: Long) {
        dao.deleteWhiteboard(id)
    }

    // --- JSON Serialization Helpers ---

    fun serializeStrokes(strokes: List<Stroke>): String {
        val array = JSONArray()
        for (stroke in strokes) {
            val obj = JSONObject()
            obj.put("id", stroke.id)
            obj.put("color", stroke.color)
            obj.put("strokeWidth", stroke.strokeWidth.toDouble())
            obj.put("penType", stroke.penType.name)
            obj.put("isHighlighter", stroke.isHighlighter)
            val pointsArr = JSONArray()
            for (pt in stroke.points) {
                val ptObj = JSONObject()
                ptObj.put("x", pt.x.toDouble())
                ptObj.put("y", pt.y.toDouble())
                ptObj.put("pressure", pt.pressure.toDouble())
                pointsArr.put(ptObj)
            }
            obj.put("points", pointsArr)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeStrokes(jsonStr: String): List<Stroke> {
        val list = mutableListOf<Stroke>()
        if (jsonStr.isBlank() || jsonStr == "[]") return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                val color = obj.optLong("color")
                val strokeWidth = obj.optDouble("strokeWidth", 4.0).toFloat()
                val penTypeStr = obj.optString("penType", PenType.BALLPOINT.name)
                val penType = try { PenType.valueOf(penTypeStr) } catch (_: Exception) { PenType.BALLPOINT }
                val isHighlighter = obj.optBoolean("isHighlighter", false)

                val pointsArr = obj.optJSONArray("points") ?: JSONArray()
                val points = mutableListOf<PointF>()
                for (j in 0 until pointsArr.length()) {
                    val pObj = pointsArr.getJSONObject(j)
                    points.add(
                        PointF(
                            x = pObj.optDouble("x", 0.0).toFloat(),
                            y = pObj.optDouble("y", 0.0).toFloat(),
                            pressure = pObj.optDouble("pressure", 1.0).toFloat()
                        )
                    )
                }
                list.add(
                    Stroke(
                        id = id,
                        points = points,
                        color = color,
                        strokeWidth = strokeWidth,
                        penType = penType,
                        isHighlighter = isHighlighter
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeShapes(shapes: List<ShapeObject>): String {
        val array = JSONArray()
        for (shape in shapes) {
            val obj = JSONObject()
            obj.put("id", shape.id)
            obj.put("type", shape.type.name)
            obj.put("startX", shape.startX.toDouble())
            obj.put("startY", shape.startY.toDouble())
            obj.put("endX", shape.endX.toDouble())
            obj.put("endY", shape.endY.toDouble())
            obj.put("strokeColor", shape.strokeColor)
            obj.put("strokeWidth", shape.strokeWidth.toDouble())
            obj.put("fillColor", shape.fillColor)
            obj.put("isFilled", shape.isFilled)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeShapes(jsonStr: String): List<ShapeObject> {
        val list = mutableListOf<ShapeObject>()
        if (jsonStr.isBlank() || jsonStr == "[]") return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                val typeStr = obj.optString("type", ShapeType.RECTANGLE.name)
                val type = try { ShapeType.valueOf(typeStr) } catch (_: Exception) { ShapeType.RECTANGLE }
                val startX = obj.optDouble("startX", 0.0).toFloat()
                val startY = obj.optDouble("startY", 0.0).toFloat()
                val endX = obj.optDouble("endX", 0.0).toFloat()
                val endY = obj.optDouble("endY", 0.0).toFloat()
                val strokeColor = obj.optLong("strokeColor", 0xFFFFFFFFL)
                val strokeWidth = obj.optDouble("strokeWidth", 4.0).toFloat()
                val fillColor = obj.optLong("fillColor", 0x00000000L)
                val isFilled = obj.optBoolean("isFilled", false)

                list.add(
                    ShapeObject(
                        id = id,
                        type = type,
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth,
                        fillColor = fillColor,
                        isFilled = isFilled
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeTextBlocks(blocks: List<TextBlockObject>): String {
        val array = JSONArray()
        for (b in blocks) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("text", b.text)
            obj.put("x", b.x.toDouble())
            obj.put("y", b.y.toDouble())
            obj.put("fontSize", b.fontSize.toDouble())
            obj.put("color", b.color)
            obj.put("isMath", b.isMath)
            obj.put("latex", b.latex)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeTextBlocks(jsonStr: String): List<TextBlockObject> {
        val list = mutableListOf<TextBlockObject>()
        if (jsonStr.isBlank() || jsonStr == "[]") return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TextBlockObject(
                        id = obj.optString("id"),
                        text = obj.optString("text"),
                        x = obj.optDouble("x", 0.0).toFloat(),
                        y = obj.optDouble("y", 0.0).toFloat(),
                        fontSize = obj.optDouble("fontSize", 20.0).toFloat(),
                        color = obj.optLong("color", 0xFFFFFFFFL),
                        isMath = obj.optBoolean("isMath", false),
                        latex = obj.optString("latex", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeGraphs(graphs: List<GraphObject>): String {
        val array = JSONArray()
        for (g in graphs) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("title", g.title)
            obj.put("formula", g.formula)
            obj.put("chartType", g.chartType.name)
            obj.put("xMin", g.xMin.toDouble())
            obj.put("xMax", g.xMax.toDouble())
            obj.put("yMin", g.yMin.toDouble())
            obj.put("yMax", g.yMax.toDouble())
            obj.put("x", g.x.toDouble())
            obj.put("y", g.y.toDouble())
            obj.put("width", g.width.toDouble())
            obj.put("height", g.height.toDouble())

            val labels = JSONArray()
            g.dataLabels.forEach { labels.put(it) }
            obj.put("dataLabels", labels)

            val values = JSONArray()
            g.dataValues.forEach { values.put(it.toDouble()) }
            obj.put("dataValues", values)

            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeGraphs(jsonStr: String): List<GraphObject> {
        val list = mutableListOf<GraphObject>()
        if (jsonStr.isBlank() || jsonStr == "[]") return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val chartTypeStr = obj.optString("chartType", ChartType.FUNCTION.name)
                val chartType = try { ChartType.valueOf(chartTypeStr) } catch (_: Exception) { ChartType.FUNCTION }

                val labelsArr = obj.optJSONArray("dataLabels") ?: JSONArray()
                val labels = (0 until labelsArr.length()).map { labelsArr.getString(it) }

                val valuesArr = obj.optJSONArray("dataValues") ?: JSONArray()
                val values = (0 until valuesArr.length()).map { valuesArr.getDouble(it).toFloat() }

                list.add(
                    GraphObject(
                        id = obj.optString("id"),
                        title = obj.optString("title", "Plot"),
                        formula = obj.optString("formula", "y = x^2"),
                        chartType = chartType,
                        xMin = obj.optDouble("xMin", -10.0).toFloat(),
                        xMax = obj.optDouble("xMax", 10.0).toFloat(),
                        yMin = obj.optDouble("yMin", -10.0).toFloat(),
                        yMax = obj.optDouble("yMax", 10.0).toFloat(),
                        dataLabels = if (labels.isNotEmpty()) labels else listOf("A", "B", "C", "D"),
                        dataValues = if (values.isNotEmpty()) values else listOf(20f, 40f, 60f, 80f),
                        x = obj.optDouble("x", 100.0).toFloat(),
                        y = obj.optDouble("y", 100.0).toFloat(),
                        width = obj.optDouble("width", 400.0).toFloat(),
                        height = obj.optDouble("height", 300.0).toFloat()
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
