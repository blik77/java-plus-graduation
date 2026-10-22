package evm.event.model;

// Жизненный цикл события
public enum EventState {
    PENDING,    // создано, ожидает публикации
    PUBLISHED,  // опубликовано администратором
    CANCELED    // отменено администратором или инициатором
}
