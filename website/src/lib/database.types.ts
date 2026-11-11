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
      doctor_profiles: {
        Row: {
          admin_notes: string | null
          bio: string | null
          created_at: string
          license_number: string
          rejection_reason: string | null
          reviewed_at: string | null
          reviewed_by: string | null
          specialization: string
          status: Database["public"]["Enums"]["doctor_status"]
          updated_at: string
          user_id: string
          years_experience: number
        }
        Insert: {
          admin_notes?: string | null
          bio?: string | null
          created_at?: string
          license_number: string
          rejection_reason?: string | null
          reviewed_at?: string | null
          reviewed_by?: string | null
          specialization: string
          status?: Database["public"]["Enums"]["doctor_status"]
          updated_at?: string
          user_id: string
          years_experience?: number
        }
        Update: {
          admin_notes?: string | null
          bio?: string | null
          created_at?: string
          license_number?: string
          rejection_reason?: string | null
          reviewed_at?: string | null
          reviewed_by?: string | null
          specialization?: string
          status?: Database["public"]["Enums"]["doctor_status"]
          updated_at?: string
          user_id?: string
          years_experience?: number
        }
        Relationships: [
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
            referencedRelation: "profiles"
            referencedColumns: ["id"]
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
          end_time: string
          id: string
          is_active: boolean
          slot_duration_minutes: number
          start_time: string
        }
        Insert: {
          created_at?: string
          day_of_week: number
          doctor_id: string
          end_time: string
          id?: string
          is_active?: boolean
          slot_duration_minutes?: number
          start_time: string
        }
        Update: {
          created_at?: string
          day_of_week?: number
          doctor_id?: string
          end_time?: string
          id?: string
          is_active?: boolean
          slot_duration_minutes?: number
          start_time?: string
        }
        Relationships: [
          {
            foreignKeyName: "doctor_weekly_availability_doctor_id_fkey"
            columns: ["doctor_id"]
            isOneToOne: false
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
        ]
      }
      notifications: {
        Row: {
          body: string
          created_at: string
          id: string
          read_at: string | null
          related_request_id: string | null
          title: string
          type: string
          user_id: string
        }
        Insert: {
          body: string
          created_at?: string
          id?: string
          read_at?: string | null
          related_request_id?: string | null
          title: string
          type: string
          user_id: string
        }
        Update: {
          body?: string
          created_at?: string
          id?: string
          read_at?: string | null
          related_request_id?: string | null
          title?: string
          type?: string
          user_id?: string
        }
        Relationships: [
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
            referencedRelation: "profiles"
            referencedColumns: ["id"]
          },
        ]
      }
      profiles: {
        Row: {
          created_at: string
          full_name: string
          id: string
          phone: string | null
          role: Database["public"]["Enums"]["user_role"]
          updated_at: string
        }
        Insert: {
          created_at?: string
          full_name: string
          id: string
          phone?: string | null
          role?: Database["public"]["Enums"]["user_role"]
          updated_at?: string
        }
        Update: {
          created_at?: string
          full_name?: string
          id?: string
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
            referencedRelation: "profiles"
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
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          patient_id: string
          patient_notes: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
        Insert: {
          created_at?: string
          doctor_id?: string | null
          doctor_response_note?: string | null
          id?: string
          patient_id: string
          patient_notes?: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status?: Database["public"]["Enums"]["request_status"]
          updated_at?: string
        }
        Update: {
          created_at?: string
          doctor_id?: string | null
          doctor_response_note?: string | null
          id?: string
          patient_id?: string
          patient_notes?: string | null
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
            referencedRelation: "profiles"
            referencedColumns: ["id"]
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
          sort_order: number
        }
        Insert: {
          created_at?: string
          description?: string | null
          icon?: string | null
          id?: string
          is_active?: boolean
          name: string
          sort_order?: number
        }
        Update: {
          created_at?: string
          description?: string | null
          icon?: string | null
          id?: string
          is_active?: boolean
          name?: string
          sort_order?: number
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
          name: string
          requires_doctor: boolean
          sort_order: number
        }
        Insert: {
          category_id: string
          created_at?: string
          description?: string | null
          duration_minutes: number
          id?: string
          is_active?: boolean
          name: string
          requires_doctor?: boolean
          sort_order?: number
        }
        Update: {
          category_id?: string
          created_at?: string
          description?: string | null
          duration_minutes?: number
          id?: string
          is_active?: boolean
          name?: string
          requires_doctor?: boolean
          sort_order?: number
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
      [_ in never]: never
    }
    Functions: {
      admin_assign_admin_role: {
        Args: { p_user_id: string }
        Returns: undefined
      }
      admin_set_doctor_status: {
        Args: {
          p_doctor_id: string
          p_new_status: Database["public"]["Enums"]["doctor_status"]
          p_reason?: string
        }
        Returns: {
          admin_notes: string | null
          bio: string | null
          created_at: string
          license_number: string
          rejection_reason: string | null
          reviewed_at: string | null
          reviewed_by: string | null
          specialization: string
          status: Database["public"]["Enums"]["doctor_status"]
          updated_at: string
          user_id: string
          years_experience: number
        }
      }
      cancel_request: {
        Args: { p_reason?: string; p_request_id: string }
        Returns: {
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          patient_id: string
          patient_notes: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
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
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          patient_id: string
          patient_notes: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
      }
      doctor_complete_request: {
        Args: { p_request_id: string }
        Returns: {
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          patient_id: string
          patient_notes: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
      }
      doctor_confirm_request: {
        Args: { p_request_id: string }
        Returns: {
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          patient_id: string
          patient_notes: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
      }
      doctor_respond_request: {
        Args: { p_accept: boolean; p_reason?: string; p_request_id: string }
        Returns: {
          created_at: string
          doctor_id: string | null
          doctor_response_note: string | null
          id: string
          patient_id: string
          patient_notes: string | null
          scheduled_end: string
          scheduled_start: string
          service_id: string
          status: Database["public"]["Enums"]["request_status"]
          updated_at: string
        }
      }
      get_available_slots: {
        Args: { p_date: string; p_doctor_id: string; p_service_id: string }
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
      is_admin: { Args: never; Returns: boolean }
      is_approved_doctor: { Args: { p_doctor_id: string }; Returns: boolean }
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
      mark_notification_read: {
        Args: { p_notification_id: string }
        Returns: {
          body: string
          created_at: string
          id: string
          read_at: string | null
          related_request_id: string | null
          title: string
          type: string
          user_id: string
        }
      }
      set_doctor_services: {
        Args: { p_service_ids: string[] }
        Returns: {
          created_at: string
          doctor_id: string
          service_id: string
        }[]
      }
      set_weekly_availability: {
        Args: { p_schedule: Json }
        Returns: {
          created_at: string
          day_of_week: number
          doctor_id: string
          end_time: string
          id: string
          is_active: boolean
          slot_duration_minutes: number
          start_time: string
        }[]
      }
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
          admin_notes: string | null
          bio: string | null
          created_at: string
          license_number: string
          rejection_reason: string | null
          reviewed_at: string | null
          reviewed_by: string | null
          specialization: string
          status: Database["public"]["Enums"]["doctor_status"]
          updated_at: string
          user_id: string
          years_experience: number
        }
      }
    }
    Enums: {
      doctor_status: "pending" | "approved" | "rejected" | "suspended"
      request_status:
        | "requested"
        | "accepted"
        | "confirmed"
        | "completed"
        | "rejected"
        | "cancelled"
      user_role: "user" | "doctor" | "admin"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DefaultSchema = Database["public"]

// Single-schema (public) convenience accessors -- this project only ever
// talks to the public schema, so the multi-schema generic overload that
// supabase gen types normally emits is dropped.
export type Tables<TableName extends keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])> =
  (DefaultSchema["Tables"] & DefaultSchema["Views"])[TableName] extends { Row: infer R } ? R : never

export type Enums<EnumName extends keyof DefaultSchema["Enums"]> = DefaultSchema["Enums"][EnumName]
