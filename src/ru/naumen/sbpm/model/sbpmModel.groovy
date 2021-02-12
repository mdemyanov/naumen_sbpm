package ru.naumen.sbpm.model
/*! UTF8 */
//Автор: mdemyanov,vsapozhnikova
//Дата создания: 05.02.2021
//Код: 
//Назначение:
/**
 * sbpmModel
 */
//Версия: 4.11.*
//Категория:
//Параметры------------------------------------------------------
//Функции--------------------------------------------------------
//Основной блок -------------------------------------------------
/*
 * Объектная модель
 */
class MetaStorage{
    Clazz clazz
}

// это типы класса metaStorage?? Может тут наследование? 
class Clazz {
    Kase[] kases
    Attribute[] attributes
    Status[] statuses
    Status[] activeStatuses //тогда такого типа нет
}

class Kase {
    String metaCode
    String title
    Attribute[] attributes //такого атрибута нет
    Status[] states //такого атрибута нет
    Status[] activeStates
}
class Attribute{
    String metaCode
    String metaClass //КАКОЙ ЭТО ТИП ДАННЫХ?
    String title

}

class Status {
    String metaCode //Почему не просто code?
    String title
}


/*
 *  Справочники (опер.)
 * это пользовательский класс
 * Почему событие (смена статуса) отдельный атрибут? Нужен ли отдельный класс?
 * Чем code отличается от metaCode ??
 * типа StateEvent нет в ОМ сейчас!
 */
class Catalogs{
    ResolutionCode resolutionCode
    Event event
    StateEvent stateEvent
}

class ResolutionCode{
    String code
    String title
    String description
}
class Event{
    String code
    String title
    String description
}
class StateEvent{
    String code
    String title
    Status[] sourceStates //правильно что ссылаюсь к классу Status ом?
    ResolutionCode sourceResolutionCode //правильно что использую класс ResolutionCode?
}

/*
 * Справочники(техн.)
 * тут же списки должны быть?
 */
class UserCatalogs{
    CatalogsElement[] multiplySource //источник многоэкземплярности шагов
    CatalogsElement prepare //Предварительная обработка параметров
    CatalogsElement[] actionType //Тип действия
}
class CatalogsElement{
    String code
    String tile
    String parent
}

/*
 * соответствие атрибутов
 * пока вобще нет понимание откуда это берется=)
 */

//-----------------------------------------------------------------------------------------------------
/*
 * Маршрут
 */
class Route{
    String idHolder //такого атрибута нет
    ResolutionCode[] resolutionCode
    Kase baseKase
    Attribute resultAttribute //такого атрибута нет "Атрибут для хранения результата"
    String title
    String note
}
/*
 * Шаблон шага
 */
class Template{
    String idHolder //такого атрибута нет
    Route route
    String title
    //Атрибут "Тип шага"
    Kase kase //не уверена что здесь kase. Атрибут "Тип шага"
    // Атрибут "Атрибут связи с головным объектом"
    Attribute sourceAttr
    //Атрибут "Атрибут для хранения результата"
    Attribute resultAttr
    String note
    String coordPoint
    ResolutionCode[] resolutionCode
    //ПАРАМЕТРЫ ДЛЯ ШАГА из группы атрибутов, что это где взять?
}

//Почему "Действие с атрибутом"  у нас отдельно а остальные действия нет?

/*
 * Действие
 */
class Action{
    String idHolder //нет такого атрибута
    String metaClass //какой тип данных??
    //Атрибут "Исходный шаблон"
    Template sourceTemplate
    //Атрибут "Исходный статус"
    Status sourceState
    //Атрибут "Исходный код решения"
    ResolutionCode sourceResult
    //Атрибут "Исходное событие" //почему не просто событие?
    Event sourceEvent  // может быть и Event и StateEvent. Через наследование может?
    //Атрибут "Действие"
    // справочник actionType
    CatalogsElement action //правильно со справочником работаю??
    //Атрибут "Целевой шаблон"
    Template targetTemplate
    //Атрибут "Целевой статус"
    Status targetState
    //Атрибут "Целевой код решения"
    ResolutionCode targetResult
    Status state //кажется здесь должен быть другой тип (системный)
    String description
    String coordPoint
    String arrCoordPoint
    //Атрибут "Создать экземпляры шага"
    // справочник multiplySource
    CatalogsElement isMultiplyStep
    //Атрибут "По атрибуту головного объекта"
    Attribute multHeadAttr //Настройки ОМ
    //Атрибут "По атрибуту предыдущего шага"
    Attribute multPrevAttr
    //Атрибут "Связь с экземпляром" ?????????????
}

/*
 * Действие с атрибутом
 */
class AttributeAction{ // код Attribute уже занят ((
    String idHolder //нет такого атрибута, в схеме под вопросом
    String metaClass //какой тип данных??
    //Атрибут "Основное действие"
    Action mainAction
    //Атрибут "Исходный атрибут"
    Attribute sourceAttr
    //Атрибут "Действие"
    // справочник actionType
    CatalogsElement action //правильно со справочником работаю??
    //Атрибут "Предварительная обработка"
    //справочник prepare
    CatalogsElement prepare
    //Атрибут "Целевой атрибут"
    Attribute targetAttr //Настройки ОМ
    Status state //кажется здесь должен быть другой тип (системный)
}
