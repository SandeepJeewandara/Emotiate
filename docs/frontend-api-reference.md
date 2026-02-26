
# Emotiate Frontend API Reference


## Base conventions

- Base URL: your backend origin, for example `http://localhost:8080`
- Standard REST envelope:

```json
{
  "data": {},
  "message": "Operation completed",
  "status": 200
}
```

- Protected HTTP endpoints require `Authorization: Bearer <jwt>`
- CORS currently allows `http://localhost:*` and `http://127.0.0.1:*`
- WebSocket endpoint: `/ws/negotiation` with SockJS + STOMP
- STOMP app prefix: `/app`
- STOMP subscribe prefix: `/topic`

## Access rules

### Public

- `/api/auth/**`
- `/api/chat/**` except `GET /api/chat/sessions` and `DELETE /api/chat/sessions/{id}`
- `/api/emotion/**`
- `GET /api/package/get`
- `/ws/**`

### Admin only

- `/api/user/**`

### Admin or Staff only

- `/api/room/**`
- `/api/package/**` except public `GET /api/package/get`
- `/api/booking/**`
- `GET /api/chat/sessions`
- `DELETE /api/chat/sessions/{id}`

## Common responses

- `200 OK` for successful reads, updates, deletes, and most actions
- `201 Created` for create endpoints
- `400 Bad Request` for invalid business rules or inactive/non-active session flows
- `401 Unauthorized` for missing/invalid JWT or invalid login
- `403 Forbidden` for authenticated users without permission
- `404 Not Found` for missing entities or unknown endpoints
- `500 Internal Server Error` for unexpected failures

Error responses use the same envelope:

```json
{
  "data": null,
  "message": "Human-readable error message",
  "status": 400
}
```

## Enums used by the frontend

### `UserRole`

- `ADMIN`
- `STAFF`
- `GUEST`

### `RoomType`

- `SINGLE`
- `DOUBLE`
- `DELUXE`
- `SUITE`
- `FAMILY`

### `PackageAddOn`

- `BREAKFAST`
- `LUNCH`
- `DINNER`
- `FULL_BOARD`
- `SPA`
- `POOL_ACCESS`
- `AIRPORT_TRANSFER`
- `LATE_CHECKOUT`
- `EARLY_CHECKIN`

### `BookingStatus`

- `PENDING`
- `CONFIRMED`
- `CANCELLED`
- `NO_SHOW`

### `SessionStatus`

- `ACTIVE`
- `COMPLETED`
- `ABORTED`

### `SenderType`

- `GUEST`
- `AGENT`
- `SYSTEM`

### `MessageType`

- `TEXT`
- `PACKAGE_CARD`
- `BOOKING_CARD`
- `OPTION_BUTTONS`
- `SYSTEM_NOTICE`

### `EmotionState`

- `FRUSTRATED`
- `HESITANT`
- `NEUTRAL`
- `INTERESTED`
- `SATISFIED`
- `EXCITED`

## 1. Auth API

Base path: `/api/auth`

### `POST /api/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "secret"
}
```

Request DTO: `LoginRequestDto`

- `username: string`
- `password: string`

Response data: `AuthResponseDto`

- `token: string`
- `userId: number`
- `username: string`
- `role: "ADMIN" | "STAFF" | "GUEST"`

### `POST /api/auth/logout`

Request:

```json
{
  "username": "admin"
}
```

Request DTO: `LogoutRequestDto`

- `username: string`

Response data:

- `null`

## 2. User API

Base path: `/api/user`

Access: `ADMIN`

### `GET /api/user/get`

Query params:

- `isActive?: boolean`
- `role?: string`

Response item: `UserResponseDto`

- `id: number`
- `firstName: string`
- `username: string`
- `email: string`

### `POST /api/user/add`

Request DTO: `UserAddRequestDto`

- `firstName: string`
- `lastName: string`
- `username: string`
- `email: string`
- `phoneNumber: string`
- `password: string`
- `role: string`

### `PUT /api/user/edit/{id}`

Path params:

- `id: number`

Request DTO: `UserUpdateRequestDto`

- `firstName?: string`
- `lastName?: string`
- `username?: string`
- `email?: string`
- `phoneNumber?: string`
- `password?: string`
- `role?: string`
- `isActive?: boolean`

### `DELETE /api/user/remove/{id}`

Path params:

- `id: number`

Response data:

- `null`

## 3. Room API

Base path: `/api/room`

Access: `ADMIN` or `STAFF`

### `GET /api/room/get`

Query params:

- `isActive?: boolean`
- `roomType?: string`
- `floor?: number`

Response item: `RoomResponseDto`

- `id: number`
- `roomNumber: string`
- `floor: number`
- `roomType: "SINGLE" | "DOUBLE" | "DELUXE" | "SUITE" | "FAMILY"`
- `description: string`
- `maxOccupancy: number`
- `isActive: boolean`

### `POST /api/room/add`

Request DTO: `RoomAddRequestDto`

- `roomNumber: string`
- `floor: number`
- `roomType: string`
- `description: string`
- `maxOccupancy: number`

Notes:

- `roomType` is case-insensitive in backend parsing
- If `maxOccupancy` is omitted, backend defaults it to `2`

### `PUT /api/room/edit/{id}`

Path params:

- `id: number`

Request DTO: `RoomUpdateRequestDto`

- `roomNumber?: string`
- `floor?: number`
- `roomType?: string`
- `description?: string`
- `maxOccupancy?: number`
- `isActive?: boolean`

### `DELETE /api/room/remove/{id}`

Path params:

- `id: number`

Behavior:

- Soft delete only. The room is marked inactive.

## 4. Package API

Base path: `/api/package`

### `GET /api/package/get`

Access: public

Query params:

- `isActive?: boolean`
- `roomType?: string`

Response item: `PackageResponseDto`

- `id: number`
- `name: string`
- `description: string`
- `roomId: number`
- `roomNumber: string`
- `roomType: string`
- `lowerBoundPrice: number`
- `upperBoundPrice: number`
- `maxOccupancy: number`
- `addOns: string[]`
- `imageUrl: string`
- `isActive: boolean`

Notes:

- `roomType` is case-insensitive in backend parsing
- With no query params, backend returns all packages, including inactive ones

### `GET /api/package/get/{id}`

Access: `ADMIN` or `STAFF`

Path params:

- `id: number`

Response data:

- `PackageResponseDto`

### `POST /api/package/add`

Access: `ADMIN` or `STAFF`

Request DTO: `PackageAddRequestDto`

- `name: string`
- `description: string`
- `roomId: number`
- `lowerBoundPrice: number`
- `upperBoundPrice: number`
- `maxOccupancy?: number`
- `addOns?: string[]`
- `imageUrl?: string`

Notes:

- `upperBoundPrice` must be greater than or equal to `lowerBoundPrice`
- `roomId` must point to an active room
- If `maxOccupancy` is omitted, backend defaults it to the selected room occupancy

### `PUT /api/package/edit/{id}`

Access: `ADMIN` or `STAFF`

Path params:

- `id: number`

Request DTO: `PackageUpdateRequestDto`

- `name?: string`
- `description?: string`
- `roomId?: number`
- `lowerBoundPrice?: number`
- `upperBoundPrice?: number`
- `maxOccupancy?: number`
- `addOns?: string[]`
- `imageUrl?: string`
- `isActive?: boolean`

### `DELETE /api/package/remove/{id}`

Access: `ADMIN` or `STAFF`

Path params:

- `id: number`

Behavior:

- Soft delete only. The package is marked inactive.

## 5. Emotion API

Base path: `/api/emotion`

Access: public

### `POST /api/emotion/detect`

Request:

```json
{
  "userMessage": "I am not sure this price works for me"
}
```

Response data: `EmotionResultDto`

- `emotion: "FRUSTRATED" | "HESITANT" | "NEUTRAL" | "INTERESTED" | "SATISFIED" | "EXCITED"`
- `confidence: number`
- `reasoning: string`

## 6. Booking API

Base path: `/api/booking`

Access: `ADMIN` or `STAFF`

### `GET /api/booking/get`

Response item: `BookingResponseDto`

- `id: number`
- `reference: string`
- `guestId: number | null`
- `packageId: number | null`
- `roomId: number | null`
- `roomNumber: string | null`
- `status: "PENDING" | "CONFIRMED" | "CANCELLED" | "NO_SHOW"`
- `offeredPricePerNight: number`
- `totalPrice: number`
- `checkInDate: string`
- `checkOutDate: string`
- `totalNights: number`
- `guestCount: number | null`
- `sessionId: string | null`
- `packageName: string | null`
- `message: string`
- `createdAt: string`

### `DELETE /api/booking/remove/{id}`

Path params:

- `id: number`

Response data:

- `null`

## 7. Negotiation HTTP API

Base path: `/api/chat`

### `POST /api/chat/session/start`

Access: public

Request DTO: `StartSessionRequestDto`

- `guestName: string`
- `initialMessage: string`

Response data: `NegotiationSessionResponseDto`

- `id: number`
- `sessionId: string`
- `guestName: string`
- `status: "ACTIVE" | "COMPLETED" | "ABORTED"`
- `currentRound: number`
- `offeredPrice: number | null`
- `recommendedPackageId: number | null`
- `bookingReference: string | null`
- `startedAt: string`
- `updatedAt: string`
- `endedAt: string | null`

Important:

- `initialMessage` is accepted by the request DTO, but the current backend implementation does not process or persist it during session creation
- Frontend should call `POST /api/chat/session/message` after session creation to actually send the first guest message

### `POST /api/chat/session/message`

Access: public

Request DTO: `SendMessageRequestDto`

- `sessionId: string`
- `message: string`

Response data: `ChatMessageResponseDto`

- `id: number`
- `sessionId: string`
- `senderType: "GUEST" | "AGENT" | "SYSTEM"`
- `messageType: "TEXT" | "PACKAGE_CARD" | "BOOKING_CARD" | "OPTION_BUTTONS" | "SYSTEM_NOTICE"`
- `content: string`
- `detectedEmotion: string | null`
- `offeredPrice: number | null`
- `metadata: string | null`
- `timestamp: string`

Important:

- This endpoint returns the persisted guest message, not the agent reply
- The agent reply is pushed asynchronously to `/topic/session/{sessionId}`
- `messageType` for guest messages is always `TEXT`

### `GET /api/chat/session/{sessionId}`

Access: public

Response data:

- `NegotiationSessionResponseDto`

### `GET /api/chat/session/{sessionId}/messages`

Access: public

Response data:

- `ChatMessageResponseDto[]`

Messages are returned oldest first.

### `GET /api/chat/sessions`

Access: `ADMIN` or `STAFF`

Response data:

- `NegotiationSessionResponseDto[]`

### `GET /api/chat/sessions/active`

Access: public

Response data:

- `NegotiationSessionResponseDto[]`

Note:

- This endpoint is currently public because of the present security matcher order

### `PUT /api/chat/session/{sessionId}/abort`

Access: public

Response data:

- `NegotiationSessionResponseDto`

Behavior:

- Only works for `ACTIVE` sessions
- Sets status to `ABORTED`

### `PUT /api/chat/session/{sessionId}/complete`

Access: public

Response data:

- `NegotiationSessionResponseDto`

Behavior:

- Only works for `ACTIVE` sessions
- Sets status to `COMPLETED`

### `DELETE /api/chat/sessions/{id}`

Access: `ADMIN` or `STAFF`

Path params:

- `id: number`

Response data:

- `null`

## 8. Negotiation WebSocket API

Transport endpoint:

- `/ws/negotiation`

Client publish prefix:

- `/app`

Subscribe prefix:

- `/topic`

### Publish: `/app/session/message`

Payload:

```json
{
  "sessionId": "session-12345678",
  "message": "Can you lower the price?"
}
```

Payload DTO: `SendMessageRequestDto`

- `sessionId: string`
- `message: string`

Behavior:

- This triggers the same negotiation pipeline as the HTTP message endpoint
- Agent reply is broadcast to `/topic/session/{sessionId}`

### Publish: `/app/session/abort`

Payload:

```json
"session-12345678"
```

Behavior:

- Aborts the active session in backend logic
- No explicit STOMP acknowledgement payload is returned by the controller

### Subscribe: `/topic/session/{sessionId}`

Payload shape:

- `ChatMessageResponseDto`

This is where agent replies arrive in real time.

## Agent message metadata

`ChatMessageResponseDto.metadata` is a JSON string. Parse it before rendering cards.

### Package cards

When `messageType === "PACKAGE_CARD"`, metadata currently uses this shape:

```json
{
  "availablePackages": [
    {
      "available": true,
      "packageId": 2,
      "packageName": "Luxury Couple Retreat",
      "lowerBoundPrice": 75000.0,
      "upperBoundPrice": 85000.0,
      "addOns": ["BREAKFAST", "POOL_ACCESS", "DINNER"],
      "totalNights": 2,
      "checkInDate": "2026-03-28",
      "checkOutDate": "2026-03-30",
      "imageUrl": "https://example.com/package.jpg",
      "message": "Room is available"
    }
  ]
}
```

### Booking confirmation card

When `messageType === "BOOKING_CARD"`, metadata currently uses this shape:

```json
{
  "bookingConfirmation": {
    "reference": "EMT-367E6BCB",
    "packageName": "Luxury Couple Retreat",
    "checkInDate": "2026-03-28",
    "checkOutDate": "2026-03-30",
    "totalNights": 2,
    "pricePerNight": 85000.0,
    "totalPrice": 170000.0,
    "addOns": ["BREAKFAST", "POOL_ACCESS", "DINNER"],
    "imageUrl": "https://example.com/package.jpg",
    "guestName": "Nima"
  }
}
```

## Frontend implementation notes

### Auth

- Call `POST /api/auth/login`
- Store JWT and attach it on protected requests
- Use returned `role` for route and feature gating

### Public browsing

- Use `GET /api/package/get?isActive=true` for public package discovery
- Do not assume `GET /api/package/get/{id}` is public

### Admin/staff CRUD

- Rooms: `GET /api/room/get`, `POST /api/room/add`, `PUT /api/room/edit/{id}`, `DELETE /api/room/remove/{id}`
- Packages: `GET /api/package/get`, `GET /api/package/get/{id}`, `POST /api/package/add`, `PUT /api/package/edit/{id}`, `DELETE /api/package/remove/{id}`
- Users: `GET /api/user/get`, `POST /api/user/add`, `PUT /api/user/edit/{id}`, `DELETE /api/user/remove/{id}`
- Bookings: `GET /api/booking/get`, `DELETE /api/booking/remove/{id}`

### Chat UI

- Start a session with `POST /api/chat/session/start`
- Immediately send the first real user message with `POST /api/chat/session/message` or `/app/session/message`
- Render the HTTP response from `/session/message` as the guest's own message
- Listen on `/topic/session/{sessionId}` for the agent response
- Use `GET /api/chat/session/{sessionId}/messages` to restore history
- Parse `metadata` only when present

## Source basis

This reference was rebuilt from the current controllers, DTOs, enums, service logic, security config, WebSocket config, response wrapper, and exception handler in this repository.
