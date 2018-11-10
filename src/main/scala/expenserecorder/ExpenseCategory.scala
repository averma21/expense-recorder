package expenserecorder

object ExpenseCategory extends Enumeration {

  type ExpenseCategory = Value

  val cab : Value = Value("CAB")
  val grocery : Value = Value("GROCERY")
  val train : Value = Value("TRAIN")
  val flight : Value = Value("FLIGHT")
  val clothing : Value = Value("CLOTHING")
  val electronics : Value = Value("ELECTRONICS")
  val fuel : Value = Value("FUEL")
  val health : Value = Value("HEALTH")
  val sports: Value = Value("SPORTS")
  val outsideFood : Value = Value("OUTSIDE_FOOD")
  val donation : Value = Value("DONATION")
  val vehicleMaintenance : Value = Value("VEHICLE_MAINTENANCE")
  val homeMaintenance : Value = Value("HOME_MAINTENANCE")
  val booksAndCourses : Value = Value("BOOKS_AND_COURSES")
  val miscellaneous : Value = Value("MISCELLANEOUS")

}
