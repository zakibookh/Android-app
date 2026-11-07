package com.example.math

data class FormulaItem(
    val id: String,
    val title: String,
    val category: String,
    val latex: String,
    val displayFormula: String,
    val description: String,
    val variables: String
)

object FormulaLibrary {

    val categories = listOf(
        "Arithmetic & Algebra",
        "Geometry & Shapes",
        "Area & Perimeter",
        "Volume & Surface Area",
        "Trigonometry",
        "Calculus & Analysis",
        "Statistics & Probability",
        "Physics & Motion"
    )

    val formulas = listOf(
        // Arithmetic & Algebra
        FormulaItem(
            id = "quad_formula",
            title = "Quadratic Formula",
            category = "Arithmetic & Algebra",
            latex = "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
            displayFormula = "x = (-b ± √(b² - 4ac)) / (2a)",
            description = "Roots of standard quadratic polynomial ax² + bx + c = 0",
            variables = "a: quadratic coeff, b: linear coeff, c: constant"
        ),
        FormulaItem(
            id = "diff_squares",
            title = "Difference of Squares",
            category = "Arithmetic & Algebra",
            latex = "a^2 - b^2 = (a - b)(a + b)",
            displayFormula = "a² - b² = (a - b)(a + b)",
            description = "Factoring quadratic difference of two squares",
            variables = "a, b: real numbers or algebraic expressions"
        ),
        FormulaItem(
            id = "binomial_thm",
            title = "Binomial Expansion",
            category = "Arithmetic & Algebra",
            latex = "(a + b)^n = \\sum_{k=0}^{n} \\binom{n}{k} a^{n-k} b^k",
            displayFormula = "(a + b)ⁿ = Σ [n! / (k!(n-k)!)] · aⁿ⁻ᵏ · bᵏ",
            description = "Expansion of powers of a binomial sum",
            variables = "n: non-negative integer exponent, k: term index"
        ),
        FormulaItem(
            id = "log_product",
            title = "Logarithm Product Rule",
            category = "Arithmetic & Algebra",
            latex = "\\log_b(xy) = \\log_b(x) + \\log_b(y)",
            displayFormula = "log_b(xy) = log_b(x) + log_b(y)",
            description = "Fundamental logarithmic multiplication identity",
            variables = "b: base (b > 0, b ≠ 1), x > 0, y > 0"
        ),

        // Geometry & Shapes
        FormulaItem(
            id = "pythagorean",
            title = "Pythagorean Theorem",
            category = "Geometry & Shapes",
            latex = "a^2 + b^2 = c^2",
            displayFormula = "a² + b² = c²",
            description = "Relationship among the three sides of a right triangle",
            variables = "a, b: legs of right triangle, c: hypotenuse"
        ),
        FormulaItem(
            id = "distance_formula",
            title = "Euclidean Distance",
            category = "Geometry & Shapes",
            latex = "d = \\sqrt{(x_2 - x_1)^2 + (y_2 - y_1)^2}",
            displayFormula = "d = √((x₂ - x₁)² + (y₂ - y₁)²)",
            description = "Straight-line distance between two points in 2D Cartesian plane",
            variables = "(x₁, y₁) and (x₂, y₂): coordinates of points"
        ),
        FormulaItem(
            id = "slope_intercept",
            title = "Slope-Intercept Line Equation",
            category = "Geometry & Shapes",
            latex = "y = mx + c",
            displayFormula = "y = mx + c",
            description = "Linear equation with gradient m and y-intercept c",
            variables = "m: slope (rise/run), c: y-axis intercept point"
        ),

        // Area & Perimeter
        FormulaItem(
            id = "circle_area",
            title = "Circle Area & Circumference",
            category = "Area & Perimeter",
            latex = "A = \\pi r^2, \\quad C = 2\\pi r",
            displayFormula = "Area = πr²  |  Circumference = 2πr",
            description = "Enclosed planar area and perimeter boundary of circle",
            variables = "r: radius of circle, π ≈ 3.14159"
        ),
        FormulaItem(
            id = "triangle_area",
            title = "Triangle Area (Standard)",
            category = "Area & Perimeter",
            latex = "A = \\frac{1}{2} b h",
            displayFormula = "Area = ½ · base · height",
            description = "Area of any triangle given perpendicular height",
            variables = "b: base length, h: perpendicular height"
        ),
        FormulaItem(
            id = "trapezoid_area",
            title = "Trapezoid Area",
            category = "Area & Perimeter",
            latex = "A = \\frac{a + b}{2} \\cdot h",
            displayFormula = "Area = ½(a + b) · h",
            description = "Area of a trapezoid with parallel sides a and b",
            variables = "a, b: lengths of parallel bases, h: height"
        ),
        FormulaItem(
            id = "ellipse_area",
            title = "Ellipse Area",
            category = "Area & Perimeter",
            latex = "A = \\pi a b",
            displayFormula = "Area = π · a · b",
            description = "Planar area bounded by an ellipse",
            variables = "a: semi-major axis, b: semi-minor axis"
        ),

        // Volume & Surface Area
        FormulaItem(
            id = "sphere_volume",
            title = "Sphere Volume & Surface Area",
            category = "Volume & Surface Area",
            latex = "V = \\frac{4}{3}\\pi r^3, \\quad A = 4\\pi r^2",
            displayFormula = "Volume = ⁴⁄₃πr³  |  Surface Area = 4πr²",
            description = "Total volume and 3D surface area of a sphere",
            variables = "r: radius of sphere"
        ),
        FormulaItem(
            id = "cylinder_volume",
            title = "Cylinder Volume",
            category = "Volume & Surface Area",
            latex = "V = \\pi r^2 h, \\quad A = 2\\pi r h + 2\\pi r^2",
            displayFormula = "Volume = πr²h  |  Surface Area = 2πrh + 2πr²",
            description = "Right circular cylinder capacity and boundary area",
            variables = "r: radius of base, h: cylinder height"
        ),
        FormulaItem(
            id = "cone_volume",
            title = "Cone Volume",
            category = "Volume & Surface Area",
            latex = "V = \\frac{1}{3}\\pi r^2 h",
            displayFormula = "Volume = ⅓πr²h",
            description = "Capacity of a circular cone",
            variables = "r: base radius, h: altitude height"
        ),

        // Trigonometry
        FormulaItem(
            id = "pythagorean_identity",
            title = "Fundamental Trig Identity",
            category = "Trigonometry",
            latex = "\\sin^2(\\theta) + \\cos^2(\\theta) = 1",
            displayFormula = "sin²(θ) + cos²(θ) = 1",
            description = "Pythagorean identity connecting sine and cosine",
            variables = "θ: angle in radians or degrees"
        ),
        FormulaItem(
            id = "law_of_sines",
            title = "Law of Sines",
            category = "Trigonometry",
            latex = "\\frac{a}{\\sin(A)} = \\frac{b}{\\sin(B)} = \\frac{c}{\\sin(C)} = 2R",
            displayFormula = "a/sin(A) = b/sin(B) = c/sin(C)",
            description = "Relates triangle side lengths to sines of opposing angles",
            variables = "a, b, c: sides, A, B, C: opposite angles"
        ),
        FormulaItem(
            id = "law_of_cosines",
            title = "Law of Cosines",
            category = "Trigonometry",
            latex = "c^2 = a^2 + b^2 - 2ab\\cos(C)",
            displayFormula = "c² = a² + b² - 2ab · cos(C)",
            description = "Generalized Pythagorean theorem for non-right triangles",
            variables = "a, b, c: sides, C: angle included between a and b"
        ),
        FormulaItem(
            id = "double_angle_sine",
            title = "Double Angle Formula (Sine)",
            category = "Trigonometry",
            latex = "\\sin(2\\theta) = 2\\sin(\\theta)\\cos(\\theta)",
            displayFormula = "sin(2θ) = 2 · sin(θ) · cos(θ)",
            description = "Sine of twice an angle",
            variables = "θ: angle"
        ),

        // Calculus & Analysis
        FormulaItem(
            id = "derivative_power_rule",
            title = "Power Rule for Derivatives",
            category = "Calculus & Analysis",
            latex = "\\frac{d}{dx}[x^n] = n x^{n-1}",
            displayFormula = "d/dx [xⁿ] = n · xⁿ⁻¹",
            description = "Differentiation of polynomial terms",
            variables = "n: any real exponent number"
        ),
        FormulaItem(
            id = "product_rule",
            title = "Product Rule for Derivatives",
            category = "Calculus & Analysis",
            latex = "\\frac{d}{dx}[u \\cdot v] = u'v + uv'",
            displayFormula = "d/dx [u · v] = u'v + uv'",
            description = "Derivative of the product of two differentiable functions",
            variables = "u(x), v(x): functions of x"
        ),
        FormulaItem(
            id = "quotient_rule",
            title = "Quotient Rule for Derivatives",
            category = "Calculus & Analysis",
            latex = "\\frac{d}{dx}\\left[\\frac{u}{v}\\right] = \\frac{u'v - uv'}{v^2}",
            displayFormula = "d/dx [u/v] = (u'v - uv') / v²",
            description = "Derivative of the ratio of two functions",
            variables = "u(x), v(x): functions with v(x) ≠ 0"
        ),
        FormulaItem(
            id = "ftc",
            title = "Fundamental Theorem of Calculus",
            category = "Calculus & Analysis",
            latex = "\\int_{a}^{b} f(x)dx = F(b) - F(a)",
            displayFormula = "∫[a to b] f(x) dx = F(b) - F(a)",
            description = "Evaluation of definite integral via antiderivative F(x)",
            variables = "F'(x) = f(x) on interval [a, b]"
        ),
        FormulaItem(
            id = "integral_by_parts",
            title = "Integration by Parts",
            category = "Calculus & Analysis",
            latex = "\\int u\\,dv = uv - \\int v\\,du",
            displayFormula = "∫ u dv = uv - ∫ v du",
            description = "Integration technique from the product rule",
            variables = "u, v: continuous differentiable functions"
        ),

        // Statistics & Probability
        FormulaItem(
            id = "arithmetic_mean",
            title = "Arithmetic Mean (Average)",
            category = "Statistics & Probability",
            latex = "\\bar{x} = \\frac{1}{n}\\sum_{i=1}^{n} x_i",
            displayFormula = "x̄ = (Σ xᵢ) / n",
            description = "Central tendency measure of sample dataset",
            variables = "n: sample size, xᵢ: individual observations"
        ),
        FormulaItem(
            id = "variance_std_dev",
            title = "Sample Variance & Std Dev",
            category = "Statistics & Probability",
            latex = "s^2 = \\frac{1}{n-1}\\sum_{i=1}^{n} (x_i - \\bar{x})^2, \\quad s = \\sqrt{s^2}",
            displayFormula = "s² = Σ (xᵢ - x̄)² / (n - 1)  |  s = √(s²)",
            description = "Measures dispersion and variability in data",
            variables = "n: sample size, x̄: sample mean"
        ),
        FormulaItem(
            id = "bayes_theorem",
            title = "Bayes' Theorem",
            category = "Statistics & Probability",
            latex = "P(A|B) = \\frac{P(B|A) \\cdot P(A)}{P(B)}",
            displayFormula = "P(A|B) = [P(B|A) · P(A)] / P(B)",
            description = "Calculates posterior conditional probability",
            variables = "P(A|B): conditional prob of A given B, P(A): prior"
        ),

        // Physics & Motion
        FormulaItem(
            id = "newtons_second_law",
            title = "Newton's Second Law",
            category = "Physics & Motion",
            latex = "F = m \\cdot a",
            displayFormula = "F = m · a",
            description = "Net force equals mass times acceleration",
            variables = "F: net force (N), m: mass (kg), a: acceleration (m/s²)"
        ),
        FormulaItem(
            id = "kinematics_velocity",
            title = "Kinematics: Velocity",
            category = "Physics & Motion",
            latex = "v = u + at, \\quad s = ut + \\frac{1}{2}at^2",
            displayFormula = "v = u + at  |  s = ut + ½at²",
            description = "Uniform acceleration equations of linear motion",
            variables = "u: initial velocity, v: final, a: acceleration, t: time, s: displacement"
        ),
        FormulaItem(
            id = "kinetic_energy",
            title = "Kinetic & Potential Energy",
            category = "Physics & Motion",
            latex = "E_k = \\frac{1}{2}mv^2, \\quad E_p = mgh",
            displayFormula = "Ek = ½mv²  |  Ep = mgh",
            description = "Mechanical kinetic and gravitational potential energy",
            variables = "m: mass, v: velocity, g: gravity (9.8 m/s²), h: height"
        ),
        FormulaItem(
            id = "ohms_law",
            title = "Ohm's Law",
            category = "Physics & Motion",
            latex = "V = I \\cdot R, \\quad P = V \\cdot I",
            displayFormula = "V = I · R  |  P = V · I",
            description = "Electrical voltage, current, resistance, and power",
            variables = "V: volts, I: current (amperes), R: ohms (Ω), P: watts"
        )
    )
}
