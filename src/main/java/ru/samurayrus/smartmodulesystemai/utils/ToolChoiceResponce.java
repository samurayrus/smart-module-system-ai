package ru.samurayrus.smartmodulesystemai.utils;

public class ToolChoiceResponce {
//    Странный пример. Я думал tool_call  в разделе tool_calls в messages должен быть отдельно
//    User: Get me the delivery date for order 123
//    Model: <tool_call>
//    {"name": "get_delivery_date", "arguments": {"order_id": "123"}}
//    </tool_call>
//
//    "tools": [
//          {
//            "type": "function",
//            "function": {
//              "name": "search_products",
//              "description": "Search the product catalog by various criteria. Use this whenever a customer asks about product availability, pricing, or specifications.",
//              "parameters": {
//                "type": "object",
//                "properties": {
//                  "query": {
//                    "type": "string",
//                    "description": "Search terms or product name"
//                  },
//                  "category": {
//                    "type": "string",
//                    "description": "Product category to filter by",
//                    "enum": ["electronics", "clothing", "home", "outdoor"]
//                  },
//                  "max_price": {
//                    "type": "number",
//                    "description": "Maximum price in dollars"
//                  }
//                },
//                "required": ["query"],
//                "additionalProperties": false
//              }
//            }
//          }
//        ]
//
//    in choices
//          "message": {
//            "role": "assistant",
//            "tool_calls": [
//              {
//                "id": "365174485",
//                "type": "function",
//                "function": {
//                  "name": "search_products",
//                  "arguments": "{\"query\":\"dell\",\"category\":\"electronics\",\"max_price\":50}"
//                }
//              }
}
