package com.example.tools

data class ElementInfo(
    val number: Int,
    val symbol: String,
    val name: String,
    val mass: Double,
    val category: String, // "Nonmetal", "Noble Gas", "Alkali Metal", "Alkaline Earth", "Transition Metal", "Post-transition", "Metalloid", "Halogen"
    val period: Int,
    val group: Int
)

object PeriodicTableData {
    val elements = listOf(
        ElementInfo(1, "H", "Hydrogen", 1.008, "Nonmetal", 1, 1),
        ElementInfo(2, "He", "Helium", 4.0026, "Noble Gas", 1, 18),
        ElementInfo(3, "Li", "Lithium", 6.94, "Alkali Metal", 2, 1),
        ElementInfo(4, "Be", "Beryllium", 9.0122, "Alkaline Earth", 2, 2),
        ElementInfo(5, "B", "Boron", 10.81, "Metalloid", 2, 13),
        ElementInfo(6, "C", "Carbon", 12.011, "Nonmetal", 2, 14),
        ElementInfo(7, "N", "Nitrogen", 14.007, "Nonmetal", 2, 15),
        ElementInfo(8, "O", "Oxygen", 15.999, "Nonmetal", 2, 16),
        ElementInfo(9, "F", "Fluorine", 18.998, "Halogen", 2, 17),
        ElementInfo(10, "Ne", "Neon", 20.180, "Noble Gas", 2, 18),
        ElementInfo(11, "Na", "Sodium", 22.990, "Alkali Metal", 3, 1),
        ElementInfo(12, "Mg", "Magnesium", 24.305, "Alkaline Earth", 3, 2),
        ElementInfo(13, "Al", "Aluminum", 26.982, "Post-transition", 3, 13),
        ElementInfo(14, "Si", "Silicon", 28.085, "Metalloid", 3, 14),
        ElementInfo(15, "P", "Phosphorus", 30.974, "Nonmetal", 3, 15),
        ElementInfo(16, "S", "Sulfur", 32.06, "Nonmetal", 3, 16),
        ElementInfo(17, "Cl", "Chlorine", 35.45, "Halogen", 3, 17),
        ElementInfo(18, "Ar", "Argon", 39.948, "Noble Gas", 3, 18),
        ElementInfo(19, "K", "Potassium", 39.098, "Alkali Metal", 4, 1),
        ElementInfo(20, "Ca", "Calcium", 40.078, "Alkaline Earth", 4, 2),
        ElementInfo(21, "Sc", "Scandium", 44.956, "Transition Metal", 4, 3),
        ElementInfo(22, "Ti", "Titanium", 47.867, "Transition Metal", 4, 4),
        ElementInfo(23, "V", "Vanadium", 50.942, "Transition Metal", 4, 5),
        ElementInfo(24, "Cr", "Chromium", 51.996, "Transition Metal", 4, 6),
        ElementInfo(25, "Mn", "Manganese", 54.938, "Transition Metal", 4, 7),
        ElementInfo(26, "Fe", "Iron", 55.845, "Transition Metal", 4, 8),
        ElementInfo(27, "Co", "Cobalt", 58.933, "Transition Metal", 4, 9),
        ElementInfo(28, "Ni", "Nickel", 58.693, "Transition Metal", 4, 10),
        ElementInfo(29, "Cu", "Copper", 63.546, "Transition Metal", 4, 11),
        ElementInfo(30, "Zn", "Zinc", 65.38, "Transition Metal", 4, 12),
        ElementInfo(31, "Ga", "Gallium", 69.723, "Post-transition", 4, 13),
        ElementInfo(32, "Ge", "Germanium", 72.630, "Metalloid", 4, 14),
        ElementInfo(33, "As", "Arsenic", 74.922, "Metalloid", 4, 15),
        ElementInfo(34, "Se", "Selenium", 78.971, "Nonmetal", 4, 16),
        ElementInfo(35, "Br", "Bromine", 79.904, "Halogen", 4, 17),
        ElementInfo(36, "Kr", "Krypton", 83.798, "Noble Gas", 4, 18),
        ElementInfo(47, "Ag", "Silver", 107.87, "Transition Metal", 5, 11),
        ElementInfo(50, "Sn", "Tin", 118.71, "Post-transition", 5, 14),
        ElementInfo(53, "I", "Iodine", 126.90, "Halogen", 5, 17),
        ElementInfo(54, "Xe", "Xenon", 131.29, "Noble Gas", 5, 18),
        ElementInfo(79, "Au", "Gold", 196.97, "Transition Metal", 6, 11),
        ElementInfo(80, "Hg", "Mercury", 200.59, "Transition Metal", 6, 12),
        ElementInfo(82, "Pb", "Lead", 207.2, "Post-transition", 6, 14),
        ElementInfo(92, "U", "Uranium", 238.03, "Actinide", 7, 3)
    )
}
