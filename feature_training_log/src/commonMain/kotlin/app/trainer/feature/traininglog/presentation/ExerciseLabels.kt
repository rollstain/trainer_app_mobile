package app.trainer.feature.traininglog.presentation

import app.trainer.data.traininglog.Equipment
import app.trainer.data.traininglog.MuscleGroup
import app.trainer.strings.Res
import app.trainer.strings.equipment_ball
import app.trainer.strings.equipment_bands
import app.trainer.strings.equipment_barbell
import app.trainer.strings.equipment_bodyweight
import app.trainer.strings.equipment_cable
import app.trainer.strings.equipment_dumbbell
import app.trainer.strings.equipment_ez_bar
import app.trainer.strings.equipment_kettlebell
import app.trainer.strings.equipment_machine
import app.trainer.strings.equipment_other
import app.trainer.strings.muscle_abdominals
import app.trainer.strings.muscle_abductors
import app.trainer.strings.muscle_adductors
import app.trainer.strings.muscle_biceps
import app.trainer.strings.muscle_calves
import app.trainer.strings.muscle_chest
import app.trainer.strings.muscle_forearms
import app.trainer.strings.muscle_glutes
import app.trainer.strings.muscle_hamstrings
import app.trainer.strings.muscle_lats
import app.trainer.strings.muscle_lower_back
import app.trainer.strings.muscle_middle_back
import app.trainer.strings.muscle_neck
import app.trainer.strings.muscle_quadriceps
import app.trainer.strings.muscle_shoulders
import app.trainer.strings.muscle_traps
import app.trainer.strings.muscle_triceps
import org.jetbrains.compose.resources.StringResource

fun MuscleGroup.label(): StringResource = when (this) {
    MuscleGroup.CHEST -> Res.string.muscle_chest
    MuscleGroup.LATS -> Res.string.muscle_lats
    MuscleGroup.MIDDLE_BACK -> Res.string.muscle_middle_back
    MuscleGroup.LOWER_BACK -> Res.string.muscle_lower_back
    MuscleGroup.TRAPS -> Res.string.muscle_traps
    MuscleGroup.SHOULDERS -> Res.string.muscle_shoulders
    MuscleGroup.BICEPS -> Res.string.muscle_biceps
    MuscleGroup.TRICEPS -> Res.string.muscle_triceps
    MuscleGroup.FOREARMS -> Res.string.muscle_forearms
    MuscleGroup.ABDOMINALS -> Res.string.muscle_abdominals
    MuscleGroup.QUADRICEPS -> Res.string.muscle_quadriceps
    MuscleGroup.HAMSTRINGS -> Res.string.muscle_hamstrings
    MuscleGroup.GLUTES -> Res.string.muscle_glutes
    MuscleGroup.CALVES -> Res.string.muscle_calves
    MuscleGroup.ADDUCTORS -> Res.string.muscle_adductors
    MuscleGroup.ABDUCTORS -> Res.string.muscle_abductors
    MuscleGroup.NECK -> Res.string.muscle_neck
}

fun Equipment.label(): StringResource = when (this) {
    Equipment.BARBELL -> Res.string.equipment_barbell
    Equipment.DUMBBELL -> Res.string.equipment_dumbbell
    Equipment.EZ_BAR -> Res.string.equipment_ez_bar
    Equipment.KETTLEBELL -> Res.string.equipment_kettlebell
    Equipment.MACHINE -> Res.string.equipment_machine
    Equipment.CABLE -> Res.string.equipment_cable
    Equipment.BODYWEIGHT -> Res.string.equipment_bodyweight
    Equipment.BANDS -> Res.string.equipment_bands
    Equipment.BALL -> Res.string.equipment_ball
    Equipment.OTHER -> Res.string.equipment_other
}
