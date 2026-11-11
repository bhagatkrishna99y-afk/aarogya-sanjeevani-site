export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.18"
  }
  public: {
    Tables: {
      audit_log: {
        Row: {
          action: string
          actor_id: string | null
          created_at: string
          entity_id: string | null
          entity_type: string
          id: number
          new_data: Json | null
          old_data: Json | null
        }
        Insert: {
          action: string
          actor_id?: string | null
          created_at?: string
          entity_id?: string | null
          entity_type: string
          id?: never
          new_data?: Json | null
          old_data?: Json | null
        }
        Update: {
          action?: string
          actor_id?: string | null
          created_at?: string
          entity_id?: string | null
          entity_type?: string
          id?: never
          new_data?: Json | null
          old_data?: Json | null
        }
        Relationships: []
      }
      doctor_profiles: {
        Row: {
          accepting_requests: boolean
          admin_notes: string | null
          bio: string | null
          consultation_modes: Database["public"]["Enums"]["service_mode"][]
          created_at: string
          license_number: string
          qualifications: string | null
          rejection_reason: string | null
          reviewed_at: string | null
          reviewed_by: string | null
          slot_duration_minutes: number
          specialization: string
          status: Database["public"]["Enums"]["doctor_status"]
          title: string | null
          updated_at: string
          user_id: string
          years_experience: number
        }
        Insert: {
          accepting_requests?: boolean
          admin_notes?: string | null
          bio?: string | null
          consultation_modes?: Database["public"]["Enums"]["service_mode"][]
          created_at?: string
          license_number: string
          qualifications?: string | null
          rejection_reason?: string | null
          reviewed_at?: string | null
          reviewed_by?: string | null
          slot_duration_minutes?: number
          specialization: string
          status?: Database["public"]["Enums"]["doctor_status"]
          title?: string | null
          updated_at?: string
          user_id: string
          years_experience?: number
        }
        Update: {
          accepting_requests?: boolean
          admin_notes?: string | null
          bio?: string | null
          consultation_modes?: Database["public"]["Enums"]["service_mode"][]
          created_at?: string
          license_number?: string
          qualifications?: string | null
          rejection_reason?: string | null
          reviewed_at?: string | null
          reviewed_by?: string | null
          slot_duration_minutes?: number
          specialization?: string
          status?: Database["public"]["Enums"]["doctor_status"]
          title?: string | null
          updated_at?: string
          user_id?: string
          years_experience?: number
        }
        Relationships: [
          {
            foreignKeyName: "doctor_profiles_reviewed_by_fkey"
            columns: ["reviewed_by"]
            isOneToOne: false
            referencedRelation: "doctor_patient_contacts"
            referencedColumns: ["patient_id"]
          },
          {
            foreignKeyName: "doctor_profiles_reviewed_by_fkey"
            columns: ["reviewed_by"]
            isOneToOne: false
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "doctor_profiles_user_id_fkey"
            columns: ["user_id"]
            isOneToOne: true
            referencedRelation: "doctor_patient_contacts"
            referencedColumns: ["patient_id"]
          },
          {
            foreignKeyName: "doctor_profiles_user_id_fkey"
            columns: ["user_id"]
            isOneToOne: true
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
        ]
      }
      doctor_services: {
        Row: {
          created_at: string
          doctor_id: string
          service_id: string
        }
        Insert: {
          created_at?: string
          doctor_id: string
          service_id: string
        }
        Update: {
          created_at?: string
          doctor_id?: string
          service_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "doctor_services_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_profiles"
            referencedColumns: ["user_id"]
          },
          {
            foreignKeyName: "doctor_services_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "doctor_services_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctors_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "doctor_services_service_id_fkey"
            columns: ["service_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["service_id"]
          },
          {
            foreignKeyName: "doctor_services_service_id_fkey"
            columns: ["service_id"]
            isOneToOne: false
            referencedRelation: "services"
            referencedColumns: ["id"]
          },
        ]
      }
      doctor_weekly_availability: {
        Row: {
          created_at: string
          day_of_week: number
          doctor_id: string
          end_minute: number | null
          end_time: string
          id: string
          is_active: boolean
          slot_duration_minutes: number
          start_minute: number | null
          start_time: string
          updated_at: string
        }
        Insert: {
          created_at?: string
          day_of_week: number
          doctor_id: string
          end_minute?: number | null
          end_time: string
          id?: string
          is_active?: boolean
          slot_duration_minutes?: number
          start_minute?: number | null
          start_time: string
          updated_at?: string
        }
        Update: {
          created_at?: string
          day_of_week?: number
          doctor_id?: string
          end_minute?: number | null
          end_time?: string
          id?: string
          is_active?: boolean
          slot_duration_minutes?: number
          start_minute?: number | null
          start_time?: string
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "doctor_weekly_availability_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_profiles"
            referencedColumns: ["user_id"]
          },
          {
            foreignKeyName: "doctor_weekly_availability_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "doctor_weekly_availability_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctors_public"
            referencedColumns: ["doctor_id"]
          },
        ]
      }
      notifications: {
        Row: {
          body: string
          created_at: string
          id: string
          is_read: boolean | null
          read_at: string | null
          related_request_id: string | null
          title: string
          type: Database["public"]["Enums"]["notification_type"]
          user_id: string
        }
        Insert: {
          body: string
          created_at?: string
          id?: string
          is_read?: boolean | null
          read_at?: string | null
          related_request_id?: string | null
          title: string
          type: Database["public"]["Enums"]["notification_type"]
          user_id: string
        }
        Update: {
          body?: string
          created_at?: string
          id?: string
          is_read?: boolean | null
          read_at?: string | null
          related_request_id?: string | null
          title?: string
          type?: Database["public"]["Enums"]["notification_type"]
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "notifications_related_request_id_fkey"
            columns: ["related_request_id"]
            isOneToOne: false
            referencedRelation: "doctor_request_contacts"
            referencedColumns: ["request_id"]
          },
          {
            foreignKeyName: "notifications_related_request_id_fkey"
            columns: ["related_request_id"]
            isOneToOne: false
            referencedRelation: "doctor_requests_view"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "notifications_related_request_id_fkey"
            columns: ["related_request_id"]
            isOneToOne: false
            referencedRelation: "patient_requests_view"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "notifications_related_request_id_fkey"
            columns: ["related_request_id"]
            isOneToOne: false
            referencedRelation: "requests"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "notifications_user_id_fkey"
            columns: ["user_id"]
            isOneToOne: false
            referencedRelation: "doctor_patient_contacts"
            referencedColumns: ["patient_id"]
          },
          {
            foreignKeyName: "notifications_user_id_fkey"
            columns: ["user_id"]
            isOneToOne: false
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
        ]
      }
      profiles: {
        Row: {
          avatar_url: string | null
          created_at: string
          email: string
          full_name: string
          id: string
          is_suspended: boolean
          phone: string | null
          role: Database["public"]["Enums"]["user_role"]
          updated_at: string
        }
        Insert: {
          avatar_url?: string | null
          created_at?: string
          email: string
          full_name: string
          id: string
          is_suspended?: boolean
          phone?: string | null
          role?: Database["public"]["Enums"]["user_role"]
          updated_at?: string
        }
        Update: {
          avatar_url?: string | null
          created_at?: string
          email?: string
          full_name?: string
          id?: string
          is_suspended?: boolean
          phone?: string | null
          role?: Database["public"]["Enums"]["user_role"]
          updated_at?: string
        }
        Relationships: []
      }
      request_status_history: {
        Row: {
          changed_at: string
          changed_by: string | null
          id: string
          new_status: Database["public"]["Enums"]["request_status"]
          note: string | null
          old_status: Database["public"]["Enums"]["request_status"] | null
          request_id: string
        }
        Insert: {
          changed_at?: string
          changed_by?: string | null
          id?: string
          new_status: Database["public"]["Enums"]["request_status"]
          note?: string | null
          old_status?: Database["public"]["Enums"]["request_status"] | null
          request_id: string
        }
        Update: {
          changed_at?: string
          changed_by?: string | null
          id?: string
          new_status?: Database["public"]["Enums"]["request_status"]
          note?: string | null
          old_status?: Database["public"]["Enums"]["request_status"] | null
          request_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "request_status_history_changed_by_fkey"
            columns: ["changed_by"]
            isOneToOne: false
            referencedRelation: "doctor_patient_contacts"
            referencedColumns: ["patient_id"]
          },
          {
            foreignKeyName: "request_status_history_changed_by_fkey"
            columns: ["changed_by"]
            isOneToOne: false
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "request_status_history_request_id_fkey"
            columns: ["request_id"]
            isOneToOne: false
            referencedRelation: "doctor_request_contacts"
            referencedColumns: ["request_id"]
          },
          {
            foreignKeyName: "request_status_history_request_id_fkey"
            columns: ["request_id"]
            isOneToOne: false
            referencedRelation: "doctor_requests_view"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "request_status_history_request_id_fkey"
            columns: ["request_id"]
            isOneToOne: false
            referencedRelation: "patient_requests_view"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "request_status_history_request_id_fkey"
            columns: ["request_id"]
            isOneToOne: false
            referencedRelation: "requests"
            referencedColumns: ["id"]
          },
        ]
      }
      requests: {
        Row: {
          accepted_at: string | null
          address_text: string | null
          cancellation_reason: string | null
          cancelled_at: string | null
          completed_at: string | null
          confirmed_at: string | null
          contact_phone: string | null
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          mode: Database["public"]["Enums"]["service_mode"]
          patient_id: string
          patient_notes: string | null
          rejected_at: string | null
          rejection_reason: string | null
          requested_date: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
        Insert: {
          accepted_at?: string | null
          address_text?: string | null
          cancellation_reason?: string | null
          cancelled_at?: string | null
          completed_at?: string | null
          confirmed_at?: string | null
          contact_phone?: string | null
          created_at?: string
          doctor_id?: string | null
          doctor_response_note?: string | null
          id?: string
          mode?: Database["public"]["Enums"]["service_mode"]
          patient_id: string
          patient_notes?: string | null
          rejected_at?: string | null
          rejection_reason?: string | null
          requested_date?: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status?: Database["public"]["Enums"]["request_status"]
          updated_at?: string
        }
        Update: {
          accepted_at?: string | null
          address_text?: string | null
          cancellation_reason?: string | null
          cancelled_at?: string | null
          completed_at?: string | null
          confirmed_at?: string | null
          contact_phone?: string | null
          created_at?: string
          doctor_id?: string | null
          doctor_response_note?: string | null
          id?: string
          mode?: Database["public"]["Enums"]["service_mode"]
          patient_id?: string
          patient_notes?: string | null
          rejected_at?: string | null
          rejection_reason?: string | null
          requested_date?: string | null
          scheduled_end?: string
          scheduled_start?: string
          service_id?: string
          status?: Database["public"]["Enums"]["request_status"]
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_profiles"
            referencedColumns: ["user_id"]
          },
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctors_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "requests_patient_id_fkey"
            columns: ["patient_id"]
            isOneToOne: false
            referencedRelation: "doctor_patient_contacts"
            referencedColumns: ["patient_id"]
          },
          {
            foreignKeyName: "requests_patient_id_fkey"
            columns: ["patient_id"]
            isOneToOne: false
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "requests_service_id_fkey"
            columns: ["service_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["service_id"]
          },
          {
            foreignKeyName: "requests_service_id_fkey"
            columns: ["service_id"]
            isOneToOne: false
            referencedRelation: "services"
            referencedColumns: ["id"]
          },
        ]
      }
      service_categories: {
        Row: {
          created_at: string
          description: string | null
          icon: string | null
          id: string
          is_active: boolean
          name: string
          slug: string
          sort_order: number
          updated_at: string
        }
        Insert: {
          created_at?: string
          description?: string | null
          icon?: string | null
          id?: string
          is_active?: boolean
          name: string
          slug: string
          sort_order?: number
          updated_at?: string
        }
        Update: {
          created_at?: string
          description?: string | null
          icon?: string | null
          id?: string
          is_active?: boolean
          name?: string
          slug?: string
          sort_order?: number
          updated_at?: string
        }
        Relationships: []
      }
      services: {
        Row: {
          category_id: string
          created_at: string
          description: string | null
          duration_minutes: number
          id: string
          is_active: boolean
          metadata: Json
          name: string
          requires_doctor: boolean
          short_description: string | null
          slug: string
          sort_order: number
          supported_modes: Database["public"]["Enums"]["service_mode"][]
          updated_at: string
        }
        Insert: {
          category_id: string
          created_at?: string
          description?: string | null
          duration_minutes: number
          id?: string
          is_active?: boolean
          metadata?: Json
          name: string
          requires_doctor?: boolean
          short_description?: string | null
          slug: string
          sort_order?: number
          supported_modes?: Database["public"]["Enums"]["service_mode"][]
          updated_at?: string
        }
        Update: {
          category_id?: string
          created_at?: string
          description?: string | null
          duration_minutes?: number
          id?: string
          is_active?: boolean
          metadata?: Json
          name?: string
          requires_doctor?: boolean
          short_description?: string | null
          slug?: string
          sort_order?: number
          supported_modes?: Database["public"]["Enums"]["service_mode"][]
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "services_category_id_fkey"
            columns: ["category_id"]
            isOneToOne: false
            referencedRelation: "service_categories"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Views: {
      doctor_patient_contacts: {
        Row: {
          full_name: string | null
          patient_id: string | null
        }
        Relationships: []
      }
      doctor_request_contacts: {
        Row: {
          contact_phone: string | null
          doctor_id: string | null
          request_id: string | null
        }
        Insert: {
          contact_phone?: never
          doctor_id?: string | null
          request_id?: string | null
        }
        Update: {
          contact_phone?: never
          doctor_id?: string | null
          request_id?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_profiles"
            referencedColumns: ["user_id"]
          },
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctors_public"
            referencedColumns: ["doctor_id"]
          },
        ]
      }
      doctor_requests_view: {
        Row: {
          address_text: string | null
          contact_phone: string | null
          created_at: string | null
          doctor_id: string | null
          doctor_response_note: string | null
          id: string | null
          mode: Database["public"]["Enums"]["service_mode"] | null
          patient_full_name: string | null
          patient_id: string | null
          patient_notes: string | null
          scheduled_end: string | null
          scheduled_start: string | null
          service_id: string | null
          service_name: string | null
          status: Database["public"]["Enums"]["request_status"] | null
          updated_at: string | null
        }
        Relationships: [
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_profiles"
            referencedColumns: ["user_id"]
          },
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctors_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "requests_patient_id_fkey"
            columns: ["patient_id"]
            isOneToOne: false
            referencedRelation: "doctor_patient_contacts"
            referencedColumns: ["patient_id"]
          },
          {
            foreignKeyName: "requests_patient_id_fkey"
            columns: ["patient_id"]
            isOneToOne: false
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "requests_service_id_fkey"
            columns: ["service_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["service_id"]
          },
          {
            foreignKeyName: "requests_service_id_fkey"
            columns: ["service_id"]
            isOneToOne: false
            referencedRelation: "services"
            referencedColumns: ["id"]
          },
        ]
      }
      doctor_services_public: {
        Row: {
          doctor_id: string | null
          full_name: string | null
          service_id: string | null
          service_name: string | null
          service_slug: string | null
        }
        Relationships: [
          {
            foreignKeyName: "doctor_profiles_user_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: true
            referencedRelation: "doctor_patient_contacts"
            referencedColumns: ["patient_id"]
          },
          {
            foreignKeyName: "doctor_profiles_user_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: true
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
        ]
      }
      doctors_public: {
        Row: {
          accepting_requests: boolean | null
          avatar_url: string | null
          bio: string | null
          consultation_modes:
            | Database["public"]["Enums"]["service_mode"][]
            | null
          doctor_id: string | null
          full_name: string | null
          qualifications: string | null
          specialization: string | null
          title: string | null
          years_of_experience: number | null
        }
        Relationships: [
          {
            foreignKeyName: "doctor_profiles_user_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: true
            referencedRelation: "doctor_patient_contacts"
            referencedColumns: ["patient_id"]
          },
          {
            foreignKeyName: "doctor_profiles_user_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: true
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
        ]
      }
      patient_requests_view: {
        Row: {
          address_text: string | null
          cancellation_reason: string | null
          contact_phone: string | null
          created_at: string | null
          doctor_full_name: string | null
          doctor_id: string | null
          doctor_response_note: string | null
          doctor_specialization: string | null
          id: string | null
          mode: Database["public"]["Enums"]["service_mode"] | null
          patient_id: string | null
          patient_notes: string | null
          rejection_reason: string | null
          scheduled_end: string | null
          scheduled_start: string | null
          service_id: string | null
          service_name: string | null
          status: Database["public"]["Enums"]["request_status"] | null
          updated_at: string | null
        }
        Relationships: [
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_profiles"
            referencedColumns: ["user_id"]
          },
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "requests_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "doctors_public"
            referencedColumns: ["doctor_id"]
          },
          {
            foreignKeyName: "requests_patient_id_fkey"
            columns: ["patient_id"]
            isOneToOne: false
            referencedRelation: "doctor_patient_contacts"
            referencedColumns: ["patient_id"]
          },
          {
            foreignKeyName: "requests_patient_id_fkey"
            columns: ["patient_id"]
            isOneToOne: false
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "requests_service_id_fkey"
            columns: ["service_id"]
            isOneToOne: false
            referencedRelation: "doctor_services_public"
            referencedColumns: ["service_id"]
          },
          {
            foreignKeyName: "requests_service_id_fkey"
            columns: ["service_id"]
            isOneToOne: false
            referencedRelation: "services"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Functions: {
      admin_assign_admin_role: {
        Args: { p_user_id: string }
        Returns: undefined
      }
      admin_get_request_detail: {
        Args: { p_request_id: string }
        Returns: {
          address_text: string
          cancellation_reason: string
          contact_phone: string
          created_at: string
          doctor_full_name: string
          doctor_id: string
          doctor_response_note: string
          id: string
          mode: Database["public"]["Enums"]["service_mode"]
          patient_full_name: string
          patient_id: string
          patient_notes: string
          patient_phone: string
          rejection_reason: string
          scheduled_end: string
          scheduled_start: string
          service_id: string
          service_name: string
          status: Database["public"]["Enums"]["request_status"]
          status_log: Json
          updated_at: string
        }[]
      }
      admin_list_doctors: {
        Args: {
          p_limit?: number
          p_offset?: number
          p_status?: Database["public"]["Enums"]["doctor_status"]
        }
        Returns: {
          created_at: string
          email: string
          full_name: string
          is_suspended: boolean
          license_number: string
          phone: string
          reviewed_at: string
          specialization: string
          status: Database["public"]["Enums"]["doctor_status"]
          user_id: string
          years_experience: number
        }[]
      }
      admin_list_requests: {
        Args: {
          p_date_from?: string
          p_date_to?: string
          p_doctor_id?: string
          p_limit?: number
          p_offset?: number
          p_service_id?: string
          p_status?: Database["public"]["Enums"]["request_status"]
        }
        Returns: {
          created_at: string
          doctor_full_name: string
          doctor_id: string
          id: string
          mode: Database["public"]["Enums"]["service_mode"]
          patient_full_name: string
          patient_id: string
          scheduled_end: string
          scheduled_start: string
          service_id: string
          service_name: string
          status: Database["public"]["Enums"]["request_status"]
        }[]
      }
      admin_set_category_active: {
        Args: { p_category_id: string; p_is_active: boolean }
        Returns: undefined
      }
      admin_set_doctor_status: {
        Args: {
          p_doctor_id: string
          p_new_status: Database["public"]["Enums"]["doctor_status"]
          p_reason?: string
        }
        Returns: {
          accepting_requests: boolean
          admin_notes: string | null
          bio: string | null
          consultation_modes: Database["public"]["Enums"]["service_mode"][]
          created_at: string
          license_number: string
          qualifications: string | null
          rejection_reason: string | null
          reviewed_at: string | null
          reviewed_by: string | null
          slot_duration_minutes: number
          specialization: string
          status: Database["public"]["Enums"]["doctor_status"]
          title: string | null
          updated_at: string
          user_id: string
          years_experience: number
        }
        SetofOptions: {
          from: "*"
          to: "doctor_profiles"
          isOneToOne: true
          isSetofReturn: false
        }
      }
      admin_set_service_active: {
        Args: { p_is_active: boolean; p_service_id: string }
        Returns: undefined
      }
      admin_set_user_role: {
        Args: {
          p_new_role: Database["public"]["Enums"]["user_role"]
          p_user_id: string
        }
        Returns: undefined
      }
      admin_set_user_suspended: {
        Args: { p_user_id: string; p_value: boolean }
        Returns: undefined
      }
      admin_upsert_service: {
        Args: {
          p_category_id?: string
          p_description?: string
          p_duration_minutes?: number
          p_id?: string
          p_is_active?: boolean
          p_name?: string
          p_requires_doctor?: boolean
          p_short_description?: string
          p_sort_order?: number
          p_supported_modes?: Database["public"]["Enums"]["service_mode"][]
        }
        Returns: string
      }
      admin_upsert_service_category: {
        Args: {
          p_description?: string
          p_icon?: string
          p_id?: string
          p_is_active?: boolean
          p_name?: string
          p_sort_order?: number
        }
        Returns: string
      }
      booking_settings: {
        Args: never
        Returns: {
          cancellation_notice_minutes: number
          max_horizon_days: number
          min_lead_minutes: number
        }[]
      }
      cancel_request: {
        Args: { p_reason?: string; p_request_id: string }
        Returns: {
          accepted_at: string | null
          address_text: string | null
          cancellation_reason: string | null
          cancelled_at: string | null
          completed_at: string | null
          confirmed_at: string | null
          contact_phone: string | null
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          mode: Database["public"]["Enums"]["service_mode"]
          patient_id: string
          patient_notes: string | null
          rejected_at: string | null
          rejection_reason: string | null
          requested_date: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
        SetofOptions: {
          from: "*"
          to: "requests"
          isOneToOne: true
          isSetofReturn: false
        }
      }
      create_request: {
        Args: {
          p_doctor_id: string
          p_patient_notes?: string
          p_scheduled_start: string
          p_service_id: string
        }
        Returns: {
          accepted_at: string | null
          address_text: string | null
          cancellation_reason: string | null
          cancelled_at: string | null
          completed_at: string | null
          confirmed_at: string | null
          contact_phone: string | null
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          mode: Database["public"]["Enums"]["service_mode"]
          patient_id: string
          patient_notes: string | null
          rejected_at: string | null
          rejection_reason: string | null
          requested_date: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
        SetofOptions: {
          from: "*"
          to: "requests"
          isOneToOne: true
          isSetofReturn: false
        }
      }
      create_service_request: {
        Args: {
          p_address: string
          p_contact_phone: string
          p_doctor_id: string
          p_mode: Database["public"]["Enums"]["service_mode"]
          p_notes: string
          p_service_id: string
          p_slot_start: string
        }
        Returns: string
      }
      current_app_role: {
        Args: never
        Returns: Database["public"]["Enums"]["user_role"]
      }
      doctor_complete_request: {
        Args: { p_request_id: string }
        Returns: {
          accepted_at: string | null
          address_text: string | null
          cancellation_reason: string | null
          cancelled_at: string | null
          completed_at: string | null
          confirmed_at: string | null
          contact_phone: string | null
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          mode: Database["public"]["Enums"]["service_mode"]
          patient_id: string
          patient_notes: string | null
          rejected_at: string | null
          rejection_reason: string | null
          requested_date: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
        SetofOptions: {
          from: "*"
          to: "requests"
          isOneToOne: true
          isSetofReturn: false
        }
      }
      doctor_confirm_request: {
        Args: { p_request_id: string }
        Returns: {
          accepted_at: string | null
          address_text: string | null
          cancellation_reason: string | null
          cancelled_at: string | null
          completed_at: string | null
          confirmed_at: string | null
          contact_phone: string | null
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          mode: Database["public"]["Enums"]["service_mode"]
          patient_id: string
          patient_notes: string | null
          rejected_at: string | null
          rejection_reason: string | null
          requested_date: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
        SetofOptions: {
          from: "*"
          to: "requests"
          isOneToOne: true
          isSetofReturn: false
        }
      }
      doctor_respond_request: {
        Args: { p_accept: boolean; p_reason?: string; p_request_id: string }
        Returns: {
          accepted_at: string | null
          address_text: string | null
          cancellation_reason: string | null
          cancelled_at: string | null
          completed_at: string | null
          confirmed_at: string | null
          contact_phone: string | null
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          mode: Database["public"]["Enums"]["service_mode"]
          patient_id: string
          patient_notes: string | null
          rejected_at: string | null
          rejection_reason: string | null
          requested_date: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
        SetofOptions: {
          from: "*"
          to: "requests"
          isOneToOne: true
          isSetofReturn: false
        }
      }
      get_available_slots: {
        Args: { p_date: string; p_doctor_id: string; p_service_id: string }
        Returns: {
          slot_end: string
          slot_start: string
        }[]
      }
      get_doctor_open_slots: {
        Args: { p_date: string; p_doctor_id: string }
        Returns: {
          slot_end: string
          slot_start: string
        }[]
      }
      get_doctor_public_profile: {
        Args: { p_doctor_id: string }
        Returns: {
          bio: string
          doctor_id: string
          full_name: string
          specialization: string
          years_experience: number
        }[]
      }
      get_my_doctor_application: {
        Args: never
        Returns: {
          bio: string
          created_at: string
          license_number: string
          rejection_reason: string
          reviewed_at: string
          specialization: string
          status: Database["public"]["Enums"]["doctor_status"]
          updated_at: string
          user_id: string
          years_experience: number
        }[]
      }
      get_unread_notification_count: { Args: never; Returns: number }
      is_admin: { Args: never; Returns: boolean }
      is_approved_doctor: { Args: { p_doctor_id: string }; Returns: boolean }
      is_doctor: { Args: never; Returns: boolean }
      list_approved_doctors: {
        Args: { p_service_id?: string }
        Returns: {
          bio: string
          doctor_id: string
          full_name: string
          specialization: string
          years_experience: number
        }[]
      }
      log_audit: {
        Args: {
          p_action: string
          p_entity_id: string
          p_entity_type: string
          p_new_data?: Json
          p_old_data?: Json
        }
        Returns: undefined
      }
      mark_all_notifications_read: { Args: never; Returns: number }
      mark_notification_read: {
        Args: { p_notification_id: string }
        Returns: number
      }
      set_accepting_requests: { Args: { p_value: boolean }; Returns: undefined }
      set_doctor_availability: { Args: { p_windows: Json }; Returns: undefined }
      set_doctor_services: {
        Args: { p_service_ids: string[] }
        Returns: {
          created_at: string
          doctor_id: string
          service_id: string
        }[]
        SetofOptions: {
          from: "*"
          to: "doctor_services"
          isOneToOne: false
          isSetofReturn: true
        }
      }
      set_weekly_availability: {
        Args: { p_schedule: Json }
        Returns: {
          created_at: string
          day_of_week: number
          doctor_id: string
          end_minute: number | null
          end_time: string
          id: string
          is_active: boolean
          slot_duration_minutes: number
          start_minute: number | null
          start_time: string
          updated_at: string
        }[]
        SetofOptions: {
          from: "*"
          to: "doctor_weekly_availability"
          isOneToOne: false
          isSetofReturn: true
        }
      }
      slugify: { Args: { input: string }; Returns: string }
      submit_doctor_application: {
        Args: {
          p_bio: string
          p_full_name: string
          p_license_number: string
          p_phone: string
          p_specialization: string
          p_years_experience: number
        }
        Returns: {
          accepting_requests: boolean
          admin_notes: string | null
          bio: string | null
          consultation_modes: Database["public"]["Enums"]["service_mode"][]
          created_at: string
          license_number: string
          qualifications: string | null
          rejection_reason: string | null
          reviewed_at: string | null
          reviewed_by: string | null
          slot_duration_minutes: number
          specialization: string
          status: Database["public"]["Enums"]["doctor_status"]
          title: string | null
          updated_at: string
          user_id: string
          years_experience: number
        }
        SetofOptions: {
          from: "*"
          to: "doctor_profiles"
          isOneToOne: true
          isSetofReturn: false
        }
      }
      submit_doctor_profile: {
        Args: {
          p_bio: string
          p_license_number: string
          p_modes: Database["public"]["Enums"]["service_mode"][]
          p_qualifications: string
          p_slot_duration: number
          p_specialization: string
          p_title: string
          p_years: number
        }
        Returns: undefined
      }
      transition_request_status: {
        Args: {
          p_new_status: Database["public"]["Enums"]["request_status"]
          p_reason?: string
          p_request_id: string
        }
        Returns: undefined
      }
    }
    Enums: {
      doctor_status: "pending" | "approved" | "rejected" | "suspended"
      notification_type:
        | "request_created"
        | "request_accepted"
        | "request_rejected"
        | "request_confirmed"
        | "request_cancelled"
        | "request_completed"
        | "doctor_approved"
        | "doctor_rejected"
        | "doctor_suspended"
        | "system"
      request_status:
        | "requested"
        | "accepted"
        | "confirmed"
        | "completed"
        | "rejected"
        | "cancelled"
      service_mode: "in_person" | "home_visit" | "online"
      user_role: "user" | "doctor" | "admin"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends (DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never) = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends (PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never) = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {
      doctor_status: ["pending", "approved", "rejected", "suspended"],
      notification_type: [
        "request_created",
        "request_accepted",
        "request_rejected",
        "request_confirmed",
        "request_cancelled",
        "request_completed",
        "doctor_approved",
        "doctor_rejected",
        "doctor_suspended",
        "system",
      ],
      request_status: [
        "requested",
        "accepted",
        "confirmed",
        "completed",
        "rejected",
        "cancelled",
      ],
      service_mode: ["in_person", "home_visit", "online"],
      user_role: ["user", "doctor", "admin"],
    },
  },
} as const
