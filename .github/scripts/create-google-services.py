import os
import sys

def main():
    json_content = os.environ.get('GOOGLE_SERVICES_JSON')
    if not json_content:
        print("Error: GOOGLE_SERVICES_JSON environment variable is empty")
        sys.exit(1)
    
    # Simple validation to ensure it's at least looking like JSON
    json_content = json_content.strip()
    if not (json_content.startswith('{') and json_content.endswith('}')):
        print("Error: GOOGLE_SERVICES_JSON does not appear to be a valid JSON object")
        sys.exit(1)
        
    os.makedirs('app', exist_ok=True)
    with open('app/google-services.json', 'w', encoding='utf-8') as f:
        f.write(json_content)
    print("Successfully created app/google-services.json")

if __name__ == "__main__":
    main()
