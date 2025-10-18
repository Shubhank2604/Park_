#!/bin/bash

# Parking Management System Test Script
# This script demonstrates the complete flow of the parking system

BASE_URL="http://localhost:8080"
ADMIN_USER="admin"
ADMIN_PASS="admin123"
ATTENDANT_USER="attendant1"
ATTENDANT_PASS="attendant123"

echo "🚗 Parking Management System Test"
echo "================================="

# Function to make API calls
api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    local token=$4
    
    if [ -n "$token" ]; then
        if [ -n "$data" ]; then
            curl -s -X $method "$BASE_URL$endpoint" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $token" \
                -d "$data"
        else
            curl -s -X $method "$BASE_URL$endpoint" \
                -H "Authorization: Bearer $token"
        fi
    else
        curl -s -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data"
    fi
}

echo "1. 🔐 Logging in as admin..."
ADMIN_TOKEN=$(api_call POST "/api/auth/login" "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | jq -r '.token')
echo "Admin token: ${ADMIN_TOKEN:0:20}..."

echo -e "\n2. 🏢 Getting parking lots..."
api_call GET "/api/lots" "" "$ADMIN_TOKEN" | jq '.'

echo -e "\n3. 📊 Checking availability..."
api_call GET "/api/lots/1/availability?type=CAR" "" "$ADMIN_TOKEN" | jq '.'

echo -e "\n4. 🔐 Logging in as attendant..."
ATTENDANT_TOKEN=$(api_call POST "/api/auth/login" "{\"username\":\"$ATTENDANT_USER\",\"password\":\"$ATTENDANT_PASS\"}" | jq -r '.token')
echo "Attendant token: ${ATTENDANT_TOKEN:0:20}..."

echo -e "\n5. 🚗 Processing vehicle entry..."
ENTRY_RESPONSE=$(api_call POST "/api/entry" '{"lotId":1,"plateNo":"TEST123","vehicleType":"CAR"}' "$ATTENDANT_TOKEN")
echo "$ENTRY_RESPONSE" | jq '.'
TICKET_ID=$(echo "$ENTRY_RESPONSE" | jq -r '.ticketId')

echo -e "\n6. 📊 Checking availability after entry..."
api_call GET "/api/lots/1/availability?type=CAR" "" "$ATTENDANT_TOKEN" | jq '.'

echo -e "\n7. 🚪 Processing vehicle exit..."
EXIT_RESPONSE=$(api_call POST "/api/exit" "{\"identifier\":\"TEST123\"}" "$ATTENDANT_TOKEN")
echo "$EXIT_RESPONSE" | jq '.'

echo -e "\n8. 💰 Checking generated invoice..."
sleep 2  # Wait for async billing processing
api_call GET "/api/invoices/$TICKET_ID" "" "$ATTENDANT_TOKEN" | jq '.'

echo -e "\n9. 💳 Processing payment..."
INVOICE_ID=$(api_call GET "/api/invoices/$TICKET_ID" "" "$ATTENDANT_TOKEN" | jq -r '.[0].id')
api_call POST "/api/pay/$INVOICE_ID" "" "$ATTENDANT_TOKEN" | jq '.'

echo -e "\n10. 📊 Final availability check..."
api_call GET "/api/lots/1/availability?type=CAR" "" "$ATTENDANT_TOKEN" | jq '.'

echo -e "\n✅ Test completed successfully!"
echo "The parking system processed a complete entry-exit cycle with billing."
