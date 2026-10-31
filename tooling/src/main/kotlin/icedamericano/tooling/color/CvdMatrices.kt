package icedamericano.tooling.color

// Machado et al.'s severity 1.0 matrices, applied to linear sRGB:
// https://www.inf.ufrgs.br/~oliveira/pubs_files/CVD_Simulation/CVD_Simulation.html
internal val PROTANOPIA_MATRIX =
    Matrix3(
        row0 = Vector3(0.152286, 1.052583, -0.204868),
        row1 = Vector3(0.114503, 0.786281, 0.099216),
        row2 = Vector3(-0.003882, -0.048116, 1.051998),
    )

internal val DEUTERANOPIA_MATRIX =
    Matrix3(
        row0 = Vector3(0.367322, 0.860646, -0.227968),
        row1 = Vector3(0.280085, 0.672501, 0.047413),
        row2 = Vector3(-0.011820, 0.042940, 0.968881),
    )
