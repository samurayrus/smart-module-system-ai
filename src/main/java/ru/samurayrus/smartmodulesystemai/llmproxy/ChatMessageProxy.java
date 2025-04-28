package ru.samurayrus.smartmodulesystemai.llmproxy;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessageProxy {
    private String role;
    private Object content;
    //Добавить поддержку мультимодальности. Массив и несколько типов данных
//    [{"type":"text","text":"Что изображено на этой картинке? "},
//     {"type":"image_url",
//     "image_url":{
//         "url":"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAvCAYAAACrKzemAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAQESURBVGhD7ZnrThpBGIa/RRE5CAsryEFAbapNvI72b2+pF2jSRFtaIx5QoFUQUDkj2H3HITV1F5Yyu6vtPgnRGRGdd7/zSO8/fHwgB0O4+FcHAzhizYAUjUYdNzSItLa25ohlEFvEcrsXyefzkyQ9rkejEd3dNenh4WU/N0vEWlhYoHh8jRQloorkZWst7u/v6fb2ji4vK1Sr1fjuy8FUsSBKJpOmWGyVFhcX+a4xOp0OFYtlurqq8B37MU0sWQ7R1tYmeb3LfGd24JX1ep3y+WMaDO75rn2YIlYiEaeNjQy5XGIqk3a7Q0dHeWo2W3zHHoTXWclkgrJZcUIBxLmdnbcUCPj5jj0IFQsBPJNZV2OV+Fp3eXmZuTUyqV0IOxUOkUoldTOdCAKBAEsYdiFMrEQiwQ5jJqjLotFVikQifMdahIjl8XjYIcZFppnAcqNRha+sRYhY+Oc9niW+Mp9gMGhLsBcilizLqlXpm9VwOGS10v7+F9bWaIGaqlqt0t7eZyqVyqwF0gPxMRyW+co65hbL6/WqmcrDV9qgCkcLA6GKxRIT70+63Q4dH59Rr9ejQuFCreC7/CfPwYPx+1+hZcH93G43X2njcv3OkHr1lyS5Zsqk0x6QGSyoGewT//6vgAvCJSa54WPz7GIHTKfXaWnpeXxD7+jz+Zj74T2hUGhiwsCEol6/Yc23Vczd7iSTaG2yE8Uyg16vT7ncd7UVavMd8xES4P8X5har3x9MzFxmMRoN1b/d5ytrmFssZC87xMJDsjJegbnFwtgE8UMP1E+tVnumg2HwN81qWi3rxzVzi4WsBDH0GAz6bBZ1eHjErHAajcYNHRx8pXL5B/tsLVCn6RW3ZiIkwDcaDc1CE7jdS7S+nqKbm1tVhBzVanVNEWB55+dFluHQa8ZiMd0MC9EhqtUImZTiULu771htpEez2VQt7JhNPfH+lZUA6+8wLoZljksANOSbm1ndQhdCX1yU1FeR71iHsLEyxibb228mVuHD4Yjd2sDFno6IIZ6ihNk8zO8PqGv+Aw3we7ncN1tm8kJn8FtbGxSPxycedgwsZPwy2ubA1WGd19f2XJMJiVljTk8L7DbGCLAm9IlGhUJ5AvezSyggVCxYCUYxuCgVCT738vKKjW7sRKhYALEEGa1aramH5JtzAIsqFM7p5OSM79iHKfeGY3Bln82mZ76NHoNgns+f2FKAamGqWAAxKZ1OqXVTdOrcC8Aa2+2WmjF/vqire2C6WE9BXbW6qrAaC9aG4hN0u13WMqFwrVQqL+KqXgtLxXrtCA/w/zJSMBh0LMsgktrPOWIZxHHDGZBkWXYsyyBSOBx2xDKI44YzIEUiEceyDOJY1gxIiqI4lmUIol+Kj6TZaMjptwAAAABJRU5ErkJggg==",
//     "detail":"high"}
//     }
//     ]
}
