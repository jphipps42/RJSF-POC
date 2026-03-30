import { createClient } from 'https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2/+esm';

const supabaseUrl = 'https://rdhoodroqxjyecxgjfrj.supabase.co';
const supabaseAnonKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJkaG9vZHJvcXhqeWVjeGdqZnJqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzA4NTY2MTEsImV4cCI6MjA4NjQzMjYxMX0.pUq8ViK8dMW0_DwzbqS_PyMJQ6q_uMhZSd39bt6lqWs';

export const supabase = createClient(supabaseUrl, supabaseAnonKey);
